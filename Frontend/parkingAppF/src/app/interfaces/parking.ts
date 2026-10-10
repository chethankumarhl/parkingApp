export interface Parking {
    id: number;
    registrationNumber: string;
    entryDate: string;
    exitDate: string | null;
    parkedTime: string | null;
    finalCost: number | null;
}
