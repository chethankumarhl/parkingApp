import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Parking } from '../interfaces/parking';
import { Observable } from 'rxjs/internal/Observable';


@Injectable({
  providedIn: 'root',
})
export class ParkingService {
  apiUrl = 'https://parkingapp-bz8a.onrender.com/parking/all';
  addUrl = 'https://parkingapp-bz8a.onrender.com/parking/add';
  deleteUrl = 'https://parkingapp-bz8a.onrender.com/parking/delete';

  constructor(private http: HttpClient) {}
  getParkingData() {
    return this.http.get<Parking[]>(this.apiUrl);
  }

  getAvailableParking() {
    return this.http.get<Parking[]>('https://parkingapp-bz8a.onrender.com/parking/available');
  }
  getRecentlyCompleted() {
  return this.http.get<Parking[]>(
    'https://parkingapp-bz8a.onrender.com/parking/recently-completed'
  );
}

  // POST https://parkingapp-bz8a.onrender.com/parking/add
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
