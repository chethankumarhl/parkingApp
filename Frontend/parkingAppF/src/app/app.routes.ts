import { Routes } from '@angular/router';
import { Home } from './components/home/home';


//routing configuration for the application
//lazy loading of components is used to improve the performance of the application

export const routes: Routes = [
    {path: '', redirectTo: 'home', pathMatch: 'full'},
    {path: 'home', loadComponent: () => import('./components/home/home').then(m => m.Home)},
    {path:'parking', loadComponent: () => import('./components/parking/parking').then(m => m.Parking)},
    {path:'price', loadComponent: () => import('./components/price/price').then(m => m.Price)},
    {path:'calender', loadComponent: () => import('./components/calender/calender').then(m => m.Calender)},
    {path:'capture', loadComponent: () => import('./components/capture/capture').then(m => m.Capture)},
    {path: '**', redirectTo: 'home', pathMatch: 'full'} //any other route will be redirected to home page
];
