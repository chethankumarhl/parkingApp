import { Component, signal } from '@angular/core';
import { CalendarService } from '../../services/calendar-service';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';

@Component({
  selector: 'app-calender',
  imports: [DatePipe,RouterLink],
  templateUrl: './calender.html',
  styleUrl: './calender.css',
})
export class Calender {
   // Data received from backend
  CalenderData: any = signal([]);

  // Currently selected date
  selectedDate = signal('');

  todayDate = signal('');

  // Loading status
  loading = signal(true);

  constructor(private calenderService: CalendarService) {}

  ngOnInit() {

    // Get today's system date
    const today = new Date();

    const todayString = this.formatDate(today);

    // Set today's date
    this.selectedDate.set(todayString);
    this.todayDate.set(todayString);
    

    // Fetch today's data
    this.getCalenderData(todayString);
  }

  // Convert Date object to YYYY-MM-DD
  formatDate(date: Date): string {

    const year = date.getFullYear();

    const month = String(date.getMonth() + 1).padStart(2, '0');

    const day = String(date.getDate()).padStart(2, '0');

    return `${year}-${month}-${day}`;
  }

  // Get data from backend
  getCalenderData(date: string) {

    this.loading.set(true);

    this.calenderService.getCalenderData(date).subscribe({

      next: (data) => {

        console.log('Calendar data:', data);

        this.CalenderData.set(data);

        this.loading.set(false);
      },

      error: (error) => {

        console.error('Error fetching calendar data:', error);

        this.CalenderData.set([]);

        this.loading.set(false);
      }

    });
  }

  // Previous day
  previousDay() {

    const currentDate = new Date(this.selectedDate());

    currentDate.setDate(currentDate.getDate() - 1);

    const newDate = this.formatDate(currentDate);

    this.selectedDate.set(newDate);

    this.getCalenderData(newDate);
  }

  // Next day
  nextDay() {

    const currentDate = new Date(this.selectedDate());

    currentDate.setDate(currentDate.getDate() + 1);

    const newDate = this.formatDate(currentDate);

    this.selectedDate.set(newDate);

    this.getCalenderData(newDate);
  }

  // Select a specific date
selectDate(event: Event) {

  const input = event.target as HTMLInputElement;

  const newDate = input.value;

  if (!newDate) {
    return;
  }

  // Update selected date
  this.selectedDate.set(newDate);

  // Fetch data for selected date
  this.getCalenderData(newDate);
}
}
