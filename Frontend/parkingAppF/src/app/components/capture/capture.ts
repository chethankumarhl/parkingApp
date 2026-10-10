import { Component, ElementRef, OnDestroy, signal, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { CaptureService, RecognitionResponse } from '../../services/capture-service';
import { ParkingService } from '../../services/parking-service';
import { MatIcon } from '@angular/material/icon';

type CaptureMode = 'choose' | 'live' | 'upload';

@Component({
  selector: 'app-capture',
  imports: [MatIcon],
  templateUrl: './capture.html',
  styleUrl: './capture.css',
})
export class Capture implements OnDestroy {

  @ViewChild('videoElement') videoElement?: ElementRef<HTMLVideoElement>;
  @ViewChild('canvasElement') canvasElement?: ElementRef<HTMLCanvasElement>;

  mode = signal<CaptureMode>('choose');
  loading = signal(false);
  error = signal<string | null>(null);
  result = signal<RecognitionResponse | null>(null);

  previewUrl = signal<string | null>(null);

  // Adding-to-parking state
  adding = signal(false);
  addSuccess = signal<string | null>(null);
  addError = signal<string | null>(null);

  private mediaStream: MediaStream | null = null;

  constructor(
    private captureService: CaptureService,
    private parkingService: ParkingService,
    private router: Router,
  ) {}


  // Mode switching
  chooseLive(): void {
    this.resetState();
    this.mode.set('live');
    this.startCamera();
  }

  chooseUpload(): void {
    this.resetState();
    this.mode.set('upload');
  }

  backToChoose(): void {
    this.stopCamera();
    this.resetState();
    this.mode.set('choose');
  }

  private resetState(): void {
    this.error.set(null);
    this.result.set(null);
    this.previewUrl.set(null);
    this.addSuccess.set(null);
    this.addError.set(null);
  }

    // Live camera flow
  private async startCamera(): Promise<void> {
    try {
      this.mediaStream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: 'environment' },
        audio: false,
      });

      // Wait a tick so the <video> element exists in the DOM after mode switch.
      setTimeout(() => {
        if (this.videoElement && this.mediaStream) {
          this.videoElement.nativeElement.srcObject = this.mediaStream;
        }
      });
    } catch (err) {
      console.error('Could not access camera:', err);
      this.error.set('Could not access camera. Please allow camera permission.');
    }
  }

  private stopCamera(): void {
    if (this.mediaStream) {
      this.mediaStream.getTracks().forEach((track) => track.stop());
      this.mediaStream = null;
    }
  }

  captureFrame(): void {
    if (!this.videoElement || !this.canvasElement) return;

    const video = this.videoElement.nativeElement;
    const canvas = this.canvasElement.nativeElement;

    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    ctx.drawImage(video, 0, 0, canvas.width, canvas.height);

    // data:image/jpeg;base64,....
    const base64Image = canvas.toDataURL('image/jpeg');
    this.previewUrl.set(base64Image);

    this.sendBase64(base64Image);
  }

  private sendBase64(base64Image: string): void {
    this.loading.set(true);
    this.error.set(null);
    this.result.set(null);
    this.addSuccess.set(null);
    this.addError.set(null);

    this.captureService.recognizeBase64(base64Image).subscribe({
      next: (response) => {
        console.log('Plate recognition response (live capture):', response);
        this.result.set(response);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Plate recognition error (live capture):', err);
        this.error.set('Failed to recognize plate. Is the recognition service running?');
        this.loading.set(false);
      },
    });
  }


  // Photo upload flow
  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    // show a preview
    const reader = new FileReader();
    reader.onload = () => this.previewUrl.set(reader.result as string);
    reader.readAsDataURL(file);

    this.loading.set(true);
    this.error.set(null);
    this.result.set(null);
    this.addSuccess.set(null);
    this.addError.set(null);

    this.captureService.recognizeFile(file).subscribe({
      next: (response) => {
        console.log('Plate recognition response (photo upload):', response);
        this.result.set(response);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Plate recognition error (photo upload):', err);
        this.error.set('Failed to recognize plate. Is the recognition service running?');
        this.loading.set(false);
      },
    });
  }


  // Send recognized plate to the parking backend
  topPlateText(): string | null {
    const plates = this.result()?.plates;
    if (!plates || plates.length === 0) return null;

    const top = [...plates].sort(
      (a, b) => b.detection_confidence - a.detection_confidence
    )[0];

    return top.plate_text;
  }

  addToParking(): void {
    const registrationNumber = this.topPlateText();
    if (!registrationNumber) return;

    this.adding.set(true);
    this.addSuccess.set(null);
    this.addError.set(null);

    this.parkingService.addParking(registrationNumber).subscribe({
      next: (response) => {
        console.log('Added to parking:', response);
        this.addSuccess.set(`Added ${registrationNumber} to parking.`);
        this.adding.set(false);
        this.router.navigate(['/parking']);
      },
      error: (err) => {
        console.error('Error adding to parking:', err);
        this.addError.set('Failed to add vehicle to parking.');
        this.adding.set(false);
      },
    });
  }




  goHome(): void {
    this.stopCamera();
    this.router.navigate(['/home']);
  }

  ngOnDestroy(): void {
    this.stopCamera();
  }
}