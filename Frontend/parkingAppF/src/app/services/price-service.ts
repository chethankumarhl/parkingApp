import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { PriceInt } from '../interfaces/price';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class PriceService {

  private apiUrl = 'https://parkingapp-bz8a.onrender.com/price';

  constructor(private http: HttpClient) { }

  // GET all price master data
  getPriceData(): Observable<PriceInt[]> { //Observable is a type that represents data that will arrive asynchronously in the future.
    return this.http.get<PriceInt[]>(
      `${this.apiUrl}/all`
    );
  }

  // UPDATE ALL price master data
  // Sends the complete array to backend
  updateAllPrices(prices: PriceInt[]): Observable<PriceInt[]> {
    return this.http.put<PriceInt[]>(
      `${this.apiUrl}/update-all`,
      prices
    );
  }

  // DELETE one price record
  deletePrice(id: number): Observable<string> {
    return this.http.delete(
      `${this.apiUrl}/delete/${id}`,
      {
        responseType: 'text'
      }
    );
  }

  // ADD new price record
  addPrice(price: PriceInt): Observable<string> {
    return this.http.post(
      `${this.apiUrl}/add`,
      price,
      {
        responseType: 'text'
      }
    );
  }


}