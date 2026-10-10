import { Component, signal } from '@angular/core';
import { ParkingService } from '../../services/parking-service';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-parking',
  imports: [DatePipe, RouterLink,MatIconModule],
  templateUrl: './parking.html',
  styleUrl: './parking.css',
})
export class Parking {

  ParkingData: any = signal<Parking[]>([]);
  RecentlyCompleted:any = signal<Parking[]>([]);
  loading = signal(true);

  constructor(private parkingService: ParkingService) { } //to use the service in this component, we need to inject it into the constructor

  ngOnInit() {

    this.loading.set(true);

    // this.parkingService.getParkingData().subscribe({
      this.parkingService.getAvailableParking().subscribe({ // Use getAvailableParking() instead of getParkingData()

      next: (data: any[]) => {

        const sortedData = data.sort((a: any, b: any) => {
          return new Date(b.entryDate).getTime()
            - new Date(a.entryDate).getTime();//the data is being sorted
        });

        this.ParkingData.set(sortedData); 
        this.loading.set(false);
      },

      error: (error) => {

        console.error('Error fetching parking data:', error);

        this.loading.set(false);
      }

    });

     this.parkingService.getRecentlyCompleted().subscribe({
    next: (data) => {
      this.RecentlyCompleted.set(data);
      this.loading.set(false);
    },
    error: (error) => {
      console.error(error);
      this.loading.set(false);
    }
  });
    
  }
  
  deleteParking(id: number) {

    this.parkingService.deleteParking(id).subscribe({

      next: (response) => {

        console.log('Parking deleted successfully:', response);

        // Refresh the parking data after deletion
        this.ngOnInit();
      },

      error: (error) => {

        console.error('Error deleting parking:', error);
      }

    });
  }
}