import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

// 'auto' = detect Indian vs Japanese plate automatically (default)
// 'in' / 'jp' = skip detection and run only that engine (faster if you already know)
export type PlateCountry = 'auto' | 'in' | 'jp';

// Same fields as the original Indian response, plus `country`.
export interface PlateResult {
  plate_text: string;                      // "KA01AB1234"  or  "品川 300 あ 12-34"
  country: 'IN' | 'JP' | 'UNKNOWN';
  detection_confidence: number;            // 0 for Japanese plates
  ocr_confidence: number;                  // 0 for Japanese plates
  bbox: number[];                          // [] for Japanese plates
  likely_indian_format: boolean;
}

export interface RecognitionResponse {
  success: boolean;
  count: number;
  plates: PlateResult[];
  registration_number: string | null;      // text of the best plate (simplest thing to read)
  country: 'IN' | 'JP' | 'UNKNOWN' | null; // country of the best plate
  processing_time_ms: number;
  attempts: number;
  debug_image_base64: string | null;       // only for Indian results
  debug?: Record<string, unknown> | null;  // routing trace (engines run, decision, ...)
}

@Injectable({
  providedIn: 'root',
})
export class CaptureService {
  apiUrl = 'http://127.0.0.1:8000';

  constructor(private http: HttpClient) {}

  // "Photo Upload" flow -> POST /api/recognize (multipart file)
  recognizeFile(file: File, country: PlateCountry = 'auto', debug = true): Observable<RecognitionResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<RecognitionResponse>(`${this.apiUrl}/api/recognize`, formData, {
      params: { country, debug: String(debug) },
    });
  }

  // "Live Capture" flow -> POST /api/recognize-base64 (raw base64 or data URL)
  recognizeBase64(base64Image: string, country: PlateCountry = 'auto', debug = true): Observable<RecognitionResponse> {
    return this.http.post<RecognitionResponse>(
      `${this.apiUrl}/api/recognize-base64`,
      { image: base64Image },
      { params: { country, debug: String(debug) } },
    );
  }
}
