import axios from 'axios';
import {
  Customer,
  Reading,
  CustomerRequest,
  CustomersResponse,
  ReadingRequest,
  ReadingsResponse,
  CustomerWithReadingsResponse,
  KindOfMeter
} from '../types';

const API_BASE_URL = '/rest';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Customer API
export const customerApi = {
  getAll: async (): Promise<CustomersResponse> => {
    const response = await api.get('/customers');
    return response.data;
  },

  getById: async (id: string): Promise<CustomerRequest> => {
    const response = await api.get(`/customers/${id}`);
    return response.data;
  },

  create: async (customer: Customer): Promise<Customer> => {
    const response = await api.post('/customers/create', customer);
    return response.data;
  },

  update: async (customer: Customer): Promise<CustomerRequest> => {
    const customerRequest: CustomerRequest = { customer };
    const response = await api.put('/customers', customerRequest);
    return response.data;
  },

  delete: async (id: string): Promise<CustomerWithReadingsResponse> => {
    const response = await api.delete(`/customers/${id}`);
    return response.data;
  },
};

// Reading API
export const readingApi = {
  getAll: async (params?: {
    customer?: string;
    start?: string;
    end?: string;
    kindOfMeter?: KindOfMeter;
  }): Promise<ReadingsResponse> => {
    const response = await api.get('/readings', { params });
    return response.data;
  },

  getById: async (id: string): Promise<ReadingRequest> => {
    const response = await api.get(`/readings/${id}`);
    return response.data;
  },

  create: async (reading: Reading): Promise<ReadingRequest> => {
    const readingRequest: ReadingRequest = { reading };
    const response = await api.post('/readings', readingRequest);
    return response.data;
  },

  update: async (reading: Reading): Promise<ReadingRequest> => {
    const readingRequest: ReadingRequest = { reading };
    const response = await api.put('/readings', readingRequest);
    return response.data;
  },

  delete: async (id: string): Promise<ReadingRequest> => {
    const response = await api.delete(`/readings/${id}`);
    return response.data;
  },
};

export default api; 