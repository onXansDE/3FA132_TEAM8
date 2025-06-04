import React, { useEffect, useState } from 'react';
import { Plus, Edit, Trash2, Search, Filter, Users, Activity, Upload } from 'lucide-react';
import { readingApi, customerApi } from '../services/api';
import { Reading, Customer, KindOfMeter } from '../types';
import toast from 'react-hot-toast';
import ReadingForm from '../components/ReadingForm';
import ConfirmDialog from '../components/ConfirmDialog';
import CsvImport from '../components/CsvImport';
import { useSearchParams } from 'react-router-dom';

const Readings: React.FC = () => {
  const [readings, setReadings] = useState<Reading[]>([]);
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [editingReading, setEditingReading] = useState<Reading | null>(null);
  const [deletingReading, setDeletingReading] = useState<Reading | null>(null);
  const [showCsvImport, setShowCsvImport] = useState(false);
  const [filters, setFilters] = useState({
    customer: '',
    kindOfMeter: '',
    startDate: '',
    endDate: '',
  });
  const [showFilters, setShowFilters] = useState(false);

  const searchParams = useSearchParams()[0];

  useEffect(() => {
    fetchData();
  }, []);

  useEffect(() => {
    // Handle URL parameters - if customer is specified, set it in filters
    const customerIdFromUrl = searchParams.get('customer');
    if (customerIdFromUrl && customers.length > 0) {
      // Check if the customer exists in our customer list
      const customerExists = customers.find(c => c.id === customerIdFromUrl);
      if (customerExists) {
        setFilters(prev => ({ ...prev, customer: customerIdFromUrl }));
        setShowFilters(true); // Show filters so user can see the selected customer
      }
    }
  }, [searchParams, customers]);

  useEffect(() => {
    fetchReadings();
  }, [filters]);

  const fetchData = async () => {
    try {
      setLoading(true);
      // First fetch customers, then we can load readings per customer if needed
      const customersResponse = await customerApi.getAll();
      setCustomers(customersResponse.customers || []);
      
      // Initially, don't load any readings - let the user filter by customer
      // This reflects the API's customer-centric design
      setReadings([]);
    } catch (error) {
      console.error('Error fetching data:', error);
      toast.error('Failed to load customers');
    } finally {
      setLoading(false);
    }
  };

  const fetchReadings = async () => {
    try {
      const params: any = {};
      if (filters.customer) params.customer = filters.customer;
      if (filters.kindOfMeter) params.kindOfMeter = filters.kindOfMeter;
      if (filters.startDate) params.start = filters.startDate;
      if (filters.endDate) params.end = filters.endDate;

      // If no customer is selected, load readings for all customers
      if (!filters.customer && customers.length > 0) {
        // Load readings for all customers
        const readingPromises = customers.map(async (customer) => {
          try {
            const customerParams = { ...params, customer: customer.id };
            const response = await readingApi.getAll(customerParams);
            return response.readings || [];
          } catch (error) {
            console.error(`Error fetching readings for customer ${customer.id}:`, error);
            return [];
          }
        });

        const readingArrays = await Promise.all(readingPromises);
        const allReadings = readingArrays.flat();
        setReadings(allReadings);
      } else if (filters.customer) {
        // Load readings for specific customer
        const response = await readingApi.getAll(params);
        setReadings(response.readings || []);
      } else {
        // No customers available, no readings to load
        setReadings([]);
      }
    } catch (error) {
      console.error('Error fetching readings:', error);
      toast.error('Failed to load readings');
    }
  };

  const handleCreateReading = async (readingData: Reading) => {
    try {
      await readingApi.create(readingData);
      toast.success('Reading created successfully');
      setShowForm(false);
      fetchReadings();
    } catch (error) {
      console.error('Error creating reading:', error);
      toast.error('Failed to create reading');
    }
  };

  const handleUpdateReading = async (readingData: Reading) => {
    try {
      await readingApi.update(readingData);
      toast.success('Reading updated successfully');
      setEditingReading(null);
      fetchReadings();
    } catch (error) {
      console.error('Error updating reading:', error);
      toast.error('Failed to update reading');
    }
  };

  const handleDeleteReading = async (reading: Reading) => {
    try {
      await readingApi.delete(reading.id);
      toast.success('Reading deleted successfully');
      setDeletingReading(null);
      fetchReadings();
    } catch (error) {
      console.error('Error deleting reading:', error);
      toast.error('Failed to delete reading');
    }
  };

  const handleBulkImport = async (importData: Customer[] | Reading[]) => {
    // The backend handles the import, so we just need to refresh the data
    fetchReadings();
  };

  const filteredReadings = readings.filter(reading =>
    reading.customer.firstName.toLowerCase().includes(searchTerm.toLowerCase()) ||
    reading.customer.lastName.toLowerCase().includes(searchTerm.toLowerCase()) ||
    reading.meterId.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const getMeterTypeColor = (type: KindOfMeter) => {
    switch (type) {
      case KindOfMeter.STROM:
        return 'bg-yellow-100 text-yellow-800';
      case KindOfMeter.WASSER:
        return 'bg-blue-100 text-blue-800';
      case KindOfMeter.HEIZUNG:
        return 'bg-red-100 text-red-800';
      case KindOfMeter.UNBEKANNT:
        return 'bg-gray-100 text-gray-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  const clearFilters = () => {
    setFilters({
      customer: '',
      kindOfMeter: '',
      startDate: '',
      endDate: '',
    });
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  return (
    <div>
      <div className="sm:flex sm:items-center sm:justify-between mb-8">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Meter Readings</h1>
          <p className="mt-1 text-sm text-gray-500">
            Manage meter readings and consumption data - readings are loaded per customer
          </p>
        </div>
        <div className="mt-4 sm:mt-0 flex space-x-3">
          <button
            onClick={() => setShowCsvImport(true)}
            className="inline-flex items-center px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
          >
            <Upload className="h-4 w-4 mr-2" />
            Import CSV
          </button>
          <button
            onClick={() => setShowForm(true)}
            className="inline-flex items-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-primary-600 hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
          >
            <Plus className="h-4 w-4 mr-2" />
            Add Reading
          </button>
        </div>
      </div>

      {/* Search and Filters */}
      <div className="mb-6 space-y-4">
        <div className="flex flex-col sm:flex-row gap-4">
          <div className="flex-1 relative">
            <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
              <Search className="h-5 w-5 text-gray-400" />
            </div>
            <input
              type="text"
              placeholder="Search readings..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="block w-full pl-10 pr-3 py-2 border border-gray-300 rounded-md leading-5 bg-white placeholder-gray-500 focus:outline-none focus:placeholder-gray-400 focus:ring-1 focus:ring-primary-500 focus:border-primary-500"
            />
          </div>
          <button
            onClick={() => setShowFilters(!showFilters)}
            className="inline-flex items-center px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
          >
            <Filter className="h-4 w-4 mr-2" />
            Filters
          </button>
        </div>

        {showFilters && (
          <div className="bg-gray-50 p-4 rounded-lg">
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Customer
                </label>
                <select
                  value={filters.customer}
                  onChange={(e) => setFilters({ ...filters, customer: e.target.value })}
                  className="block w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                >
                  <option value="">All customers</option>
                  {customers.map((customer) => (
                    <option key={customer.id} value={customer.id}>
                      {customer.firstName} {customer.lastName}
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Meter Type
                </label>
                <select
                  value={filters.kindOfMeter}
                  onChange={(e) => setFilters({ ...filters, kindOfMeter: e.target.value })}
                  className="block w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                >
                  <option value="">All types</option>
                  <option value={KindOfMeter.STROM}>Electricity</option>
                  <option value={KindOfMeter.WASSER}>Water</option>
                  <option value={KindOfMeter.HEIZUNG}>Heating</option>
                  <option value={KindOfMeter.UNBEKANNT}>Unknown</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Start Date
                </label>
                <input
                  type="date"
                  value={filters.startDate}
                  onChange={(e) => setFilters({ ...filters, startDate: e.target.value })}
                  className="block w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  End Date
                </label>
                <input
                  type="date"
                  value={filters.endDate}
                  onChange={(e) => setFilters({ ...filters, endDate: e.target.value })}
                  className="block w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                />
              </div>
            </div>
            <div className="mt-4 flex justify-end">
              <button
                onClick={clearFilters}
                className="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
              >
                Clear Filters
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Readings Table */}
      <div className="bg-white shadow overflow-hidden sm:rounded-md">
        {filteredReadings.length === 0 ? (
          <div className="text-center py-12">
            {customers.length === 0 ? (
              <div>
                <Users className="mx-auto h-12 w-12 text-gray-400" />
                <p className="mt-2 text-gray-500">No customers found</p>
                <p className="text-sm text-gray-400">Create customers first to manage their meter readings</p>
              </div>
            ) : readings.length === 0 && !filters.customer ? (
              <div>
                <Filter className="mx-auto h-12 w-12 text-gray-400" />
                <p className="mt-2 text-gray-500">Select a customer to view readings</p>
                <p className="text-sm text-gray-400">Use the customer filter above to load readings</p>
              </div>
            ) : (
              <div>
                <Activity className="mx-auto h-12 w-12 text-gray-400" />
                <p className="mt-2 text-gray-500">No readings found</p>
                <p className="text-sm text-gray-400">Try adjusting your filters or add new readings</p>
              </div>
            )}
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Customer
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Meter ID
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Type
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Reading
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Date
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Substitute
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {filteredReadings.map((reading) => (
                  <tr key={reading.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm font-medium text-gray-900">
                        {reading.customer.firstName} {reading.customer.lastName}
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {reading.meterId}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${getMeterTypeColor(reading.kindOfMeter)}`}>
                        {reading.kindOfMeter}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {reading.meterCount}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {new Date(reading.dateOfReading).toLocaleDateString()}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {reading.substitute ? 'Yes' : 'No'}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                      <div className="flex space-x-2">
                        <button
                          onClick={() => setEditingReading(reading)}
                          className="text-primary-600 hover:text-primary-900"
                        >
                          <Edit className="h-4 w-4" />
                        </button>
                        <button
                          onClick={() => setDeletingReading(reading)}
                          className="text-red-600 hover:text-red-900"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Create Reading Form */}
      {showForm && (
        <ReadingForm
          customers={customers}
          onSubmit={handleCreateReading}
          onCancel={() => setShowForm(false)}
        />
      )}

      {/* Edit Reading Form */}
      {editingReading && (
        <ReadingForm
          reading={editingReading}
          customers={customers}
          onSubmit={handleUpdateReading}
          onCancel={() => setEditingReading(null)}
        />
      )}

      {/* Delete Confirmation */}
      {deletingReading && (
        <ConfirmDialog
          title="Delete Reading"
          message={`Are you sure you want to delete this reading for ${deletingReading.customer.firstName} ${deletingReading.customer.lastName}? This action cannot be undone.`}
          onConfirm={() => handleDeleteReading(deletingReading)}
          onCancel={() => setDeletingReading(null)}
        />
      )}

      {/* CSV Import */}
      {showCsvImport && (
        <CsvImport
          type="readings"
          customers={customers}
          onImport={handleBulkImport}
          onCancel={() => setShowCsvImport(false)}
        />
      )}
    </div>
  );
};

export default Readings; 