export enum Gender {
  D = 'D',
  M = 'M',
  U = 'U',
  W = 'W'
}

export enum KindOfMeter {
  HEIZUNG = 'HEIZUNG',
  STROM = 'STROM',
  WASSER = 'WASSER',
  UNBEKANNT = 'UNBEKANNT'
}

export interface Customer {
  id: string;
  firstName: string;
  lastName: string;
  birthDate: string;
  gender: Gender;
}

export interface Reading {
  id: string;
  comment?: string;
  customer: Customer;
  dateOfReading: string;
  kindOfMeter: KindOfMeter;
  meterCount: number;
  meterId: string;
  substitute: boolean;
}

export interface CustomerRequest {
  customer: Customer;
}

export interface CustomersResponse {
  customers: Customer[];
}

export interface ReadingRequest {
  reading: Reading;
}

export interface ReadingsResponse {
  readings: Reading[];
}

export interface CustomerWithReadingsResponse {
  customer: Customer;
  readings: Reading[];
} 