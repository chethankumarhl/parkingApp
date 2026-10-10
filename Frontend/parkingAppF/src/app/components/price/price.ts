import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  FormArray,
  Validators
} from '@angular/forms';
import { Router } from '@angular/router';
import { PriceService } from '../../services/price-service';
import { PriceInt as PriceModel } from '../../interfaces/price';

@Component({
  selector: 'app-price',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule
  ],
  templateUrl: './price.html',
  styleUrl: './price.css'
})
export class Price implements OnInit {

  priceData = signal<PriceModel[]>([]);

  loading = signal(true);

  editing = signal(false);

  saving = signal(false);

  adding = signal(false);

  addingPrice = signal(false);

  deletingId = signal<number | null>(null);


  // FORMS
  priceForm: FormGroup;

  addPriceForm: FormGroup;


  constructor(
    private priceService: PriceService,
    private fb: FormBuilder,
    private router: Router
  ) {

    // Form for existing prices
    this.priceForm = this.fb.group({
      prices: this.fb.array([])
    });


    // Form for adding a new price
    this.addPriceForm = this.fb.group({

      duration: [
        null,
        [
          Validators.required,
          Validators.min(1)
        ]
      ],

      displayDuration: [
        '',
        Validators.required
      ],

      unitPrice: [
        null,
        [
          Validators.required,
          Validators.min(0)
        ]
      ]

    });

  }


  // INITIALIZE

  ngOnInit(): void {

    this.loadPrices();

  }


  // LOAD PRICE DATA

  loadPrices(): void {

    this.loading.set(true);

    this.priceService.getPriceData().subscribe({

      next: (data) => {

        console.log('Price data:', data);

        this.setSortedData(data);

        this.loading.set(false);

      },

      error: (error) => {

        console.error(
          'Error fetching price data:',
          error
        );

        this.loading.set(false);

      }

    });

  }


  // SORT DATA

  setSortedData(data: PriceModel[]): void {

    const sortedData = [...data].sort((a, b) => {

      /*
       * duration = null means the final
       * "greater than 24 hours" category.
       */

      const durationA =
        a.duration === null
          ? Infinity
          : a.duration;

      const durationB =
        b.duration === null
          ? Infinity
          : b.duration;

      return durationA - durationB;

    });


    this.priceData.set(sortedData);

    this.createForm(sortedData);

  }



  // CREATE EDIT FORM

  createForm(data: PriceModel[]): void {

    const controls = data.map(price => {

      return this.fb.group({

        id: [price.id],

        duration: [price.duration],

        displayDuration: [price.displayDuration],

        unitPrice: [price.unitPrice]

      });

    });


    this.priceForm.setControl(
      'prices',
      this.fb.array(controls)
    );


    // Initially read-only
    this.priceForm.disable();

  }



  // FORM ARRAY

  get prices(): FormArray {

    return this.priceForm.get(
      'prices'
    ) as FormArray;

  }


  // EDIT


  editPrices(): void {

    this.editing.set(true);

    this.priceForm.enable();

  }


  // UPDATE ALL

  savePrices(): void {

    if (!this.editing()) {
      return;
    }


    this.saving.set(true);


    /*
     * getRawValue() is important because
     * disabled controls would otherwise
     * be excluded from form.value.
     */

    const updatedPrices: PriceModel[] =
      this.prices.getRawValue();


    console.log(
      'Sending updated prices:',
      updatedPrices
    );


    /*
     * Send complete array
     *
     * PUT /price/update-all
     */

    this.priceService
      .updateAllPrices(updatedPrices)
      .subscribe({

        next: (response) => {

          console.log(
            'Prices updated successfully:',
            response
          );


          this.saving.set(false);

          this.editing.set(false);

          this.priceForm.disable();


          /*
           * Update local table
           */

          this.setSortedData(response);


          /*
           * Navigate to home
           */

          this.router.navigate(['/home']);

        },

        error: (error) => {

          console.error(
            'Error updating prices:',
            error
          );

          this.saving.set(false);

        }

      });

  }


  // SHOW ADD FORM


  showAddForm(): void {

    this.adding.set(true);

    this.addPriceForm.reset();

  }


  // CANCEL ADD

  cancelAdd(): void {

    this.adding.set(false);

    this.addPriceForm.reset();

  }


  // ADD NEW PRICE

  addNewPrice(): void {

    /*
     * Check form validation
     */

    if (this.addPriceForm.invalid) {

      this.addPriceForm.markAllAsTouched();

      return;

    }


    this.addingPrice.set(true);


    /*
     * Get form values
     */

    const formValue =
      this.addPriceForm.getRawValue();


    /*
     * Create object to send to backend
     */

    const newPrice: PriceModel = {

      id: null,

      duration: Number(formValue.duration),

      displayDuration:
        formValue.displayDuration,

      unitPrice:
        Number(formValue.unitPrice)

    };


    console.log(
      'Adding price:',
      newPrice
    );


    /*
     * POST /price/add
     */

    this.priceService
      .addPrice(newPrice)
      .subscribe({

        next: (response) => {

          console.log(
            'Price added successfully:',
            response
          );


          this.addingPrice.set(false);

          this.adding.set(false);


          /*
           * Clear form
           */

          this.addPriceForm.reset();


          /*
           * Reload data from database.
           *
           * This guarantees that the table
           * contains the latest DB state and
           * is sorted correctly.
           */

          this.loadPrices();

        },

        error: (error) => {

          console.error(
            'Error adding price:',
            error
          );

          this.addingPrice.set(false);

        }

      });

  }


  // DELETE PRICE

  deletePrice(id: number | null): void {

    if (!this.editing()) {
      return;
    }


    if (id === null) {
      return;
    }


    const confirmed = window.confirm(
      'Are you sure you want to delete this price record?'
    );


    if (!confirmed) {
      return;
    }


    this.deletingId.set(id);


    /*
     * DELETE /price/delete/:id
     */

    this.priceService
      .deletePrice(id)
      .subscribe({

        next: () => {

          console.log(
            'Price deleted successfully:',
            id
          );


          /*
           * Remove from local data
           */

          const updatedData =
            this.priceData().filter(
              price => price.id !== id
            );


          this.priceData.set(
            updatedData
          );


          /*
           * Remove from FormArray
           */

          const index =
            this.prices.controls.findIndex(
              control =>
                control.get('id')?.value === id
            );


          if (index !== -1) {

            this.prices.removeAt(index);

          }


          this.deletingId.set(null);

        },

        error: (error) => {

          console.error(
            'Error deleting price:',
            error
          );

          this.deletingId.set(null);

        }

      });

  }


  // BACK

  goBack(): void {

    this.router.navigate(['/home']);

  }

}