import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

// Same shapes as before, so existing components keep working.
export interface PlateResult {
  plate_text: string;            // e.g. "品川 300 あ 12-34"
  detection_confidence: number;  // no longer provided by the backend (always 0)
  ocr_confidence: number;        // no longer provided by the backend (always 0)
  bbox: number[];                // no longer provided by the backend (always [])
  likely_indian_format: boolean; // not applicable to Japanese plates (always false)
}

export interface RecognitionResponse {
  success: boolean;
  count: number;
  plates: PlateResult[];
  processing_time_ms: number;
  attempts: number;
  debug_image_base64: string | null;
  registration_number: string | null; // NEW: simplest way to read the result
}

// What the new FastAPI service actually returns
interface OcrApiResponse {
  registration_number: string | null;
  debug?: { time_ms?: number; passes?: Record<string, string | null> };
}

@Injectable({
  providedIn: 'root',
})
export class CaptureService {
  apiUrl = 'http://127.0.0.1:8000';

  constructor(private http: HttpClient) {}

  // "Photo Upload" flow -> POST /ocr (multipart file)
  recognizeFile(file: File): Observable<RecognitionResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http
      .post<OcrApiResponse>(`${this.apiUrl}/ocr`, formData, { params: { debug: 'true' } })
      .pipe(map((r) => this.toLegacy(r)));
  }

  // "Live Capture" flow -> POST /ocr/base64 (raw base64 or data URL)
  recognizeBase64(base64Image: string): Observable<RecognitionResponse> {
    return this.http
      .post<OcrApiResponse>(`${this.apiUrl}/ocr/base64`, { image: base64Image }, { params: { debug: 'true' } })
      .pipe(map((r) => this.toLegacy(r)));
  }

  private toLegacy(r: OcrApiResponse): RecognitionResponse {
    const reg = r.registration_number;
    return {
      success: !!reg,
      count: reg ? 1 : 0,
      plates: reg
        ? [{ plate_text: reg, detection_confidence: 0, ocr_confidence: 0, bbox: [], likely_indian_format: false }]
        : [],
      processing_time_ms: r.debug?.time_ms ?? 0,
      attempts: Object.keys(r.debug?.passes ?? {}).length,
      debug_image_base64: null,
      registration_number: reg,
    };
  }
}
