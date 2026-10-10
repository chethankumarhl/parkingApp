// plate-ocr.service.ts
import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface PlateResult {
  registration_number: string | null; // e.g. "品川 300 あ 12-34"
}

@Injectable({ providedIn: 'root' })
export class PlateOcrService {
  private base = 'http://localhost:8000';
  constructor(private http: HttpClient) {}

  // Use with <input type="file"> or camera capture
  uploadFile(file: File): Observable<PlateResult> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<PlateResult>(`${this.base}/ocr`, form);
  }

  // Use if you already have a base64 / data URL string
  uploadBase64(dataUrl: string): Observable<PlateResult> {
    return this.http.post<PlateResult>(`${this.base}/ocr/base64`, { image: dataUrl });
  }
}
