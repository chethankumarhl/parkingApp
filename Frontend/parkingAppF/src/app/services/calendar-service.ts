import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class CalendarService {
   apiUrl = 'https://parkingapp-bz8a.onrender.com/calender/all';

  constructor(private http: HttpClient) {}

  getCalenderData(date: string) {

    const params = new HttpParams()
      .set('date', date);

    return this.http.get(this.apiUrl, { params });
  }
}
