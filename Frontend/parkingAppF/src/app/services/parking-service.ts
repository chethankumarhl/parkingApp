import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Parking } from '../interfaces/parking';
import { Observable } from 'rxjs/internal/Observable';


@Injectable({
  providedIn: 'root',
})
export class ParkingService {
  apiUrl = 'http://localhost:8080/parking/all';
  addUrl = 'http://localhost:8080/parking/add';
  deleteUrl = 'http://localhost:8080/parking/delete';

  constructor(private http: HttpClient) {}
  getParkingData() {
    return this.http.get<Parking[]>(this.apiUrl);
  }

  getAvailableParking() {
    return this.http.get<Parking[]>('http://localhost:8080/parking/available');
  }
  getRecentlyCompleted() {
  return this.http.get<Parking[]>(
    'http://localhost:8080/parking/recently-completed'
  );
}

  // POST http://localhost:8080/parking/add
  // Body: { "registrationNumber": "KA01NJ3112" }
   addParking(registrationNumber: string): Observable<string> {
    return this.http.post(
      this.addUrl,
      { registrationNumber },
      { responseType: 'text' }
    );
  }

 deleteParking(id: number): Observable<string> {
    return this.http.delete(
      `${this.deleteUrl}/${id}`,
      {
        responseType: 'text'
      }
    );
  }

}
