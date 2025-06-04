import React, { useEffect, useState } from 'react';
import { Search, Users, Trash2, AlertTriangle, ExternalLink } from 'lucide-react';
import { customerApi, dbSetupApi } from '../services/api';
import { Customer } from '../types';
import toast from 'react-hot-toast';
import ConfirmDialog from '../components/ConfirmDialog';
import { useNavigate } from 'react-router-dom';

const Dashboard: React.FC = () => {
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [filteredCustomers, setFilteredCustomers] = useState<Customer[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [loading, setLoading] = useState(true);
  const [showClearConfirm, setShowClearConfirm] = useState(false);
  const [isClearing, setIsClearing] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    fetchCustomers();
  }, []);

  useEffect(() => {
    // Filter customers based on search term
    if (searchTerm.trim() === '') {
      setFilteredCustomers(customers.slice(0, 20)); // Show first 20 customers by default
    } else {
      const filtered = customers.filter(customer =>
        customer.firstName.toLowerCase().includes(searchTerm.toLowerCase()) ||
        customer.lastName.toLowerCase().includes(searchTerm.toLowerCase()) ||
        `${customer.firstName} ${customer.lastName}`.toLowerCase().includes(searchTerm.toLowerCase())
      );
      setFilteredCustomers(filtered.slice(0, 50)); // Show up to 50 search results
    }
  }, [searchTerm, customers]);

  const fetchCustomers = async () => {
    try {
      setLoading(true);
      const response = await customerApi.getAll();
      setCustomers(response.customers || []);
    } catch (error) {
      console.error('Error fetching customers:', error);
      toast.error('Failed to load customers');
    } finally {
      setLoading(false);
    }
  };

  const handleClearDatabase = async () => {
    try {
      setIsClearing(true);
      const result = await dbSetupApi.resetDatabase();
      toast.success('Database cleared successfully');
      setShowClearConfirm(false);
      
      // Refresh customer data
      fetchCustomers();
    } catch (error) {
      console.error('Error clearing database:', error);
      toast.error('Failed to clear database');
    } finally {
      setIsClearing(false);
    }
  };

  const handleCustomerClick = (customerId: string) => {
    navigate(`/readings?customer=${customerId}`);
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
        <p className="ml-4 text-gray-600">Loading customers...</p>
      </div>
    );
  }

  return (
    <div>
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
        <p className="mt-1 text-sm text-gray-500">
          Search and manage your customers ({customers.length} total)
        </p>
      </div>

      {/* Customer Search */}
      <div className="bg-white shadow rounded-lg mb-8">
        <div className="px-4 py-5 sm:p-6">
          <h3 className="text-lg font-medium text-gray-900 mb-4">
            Find Customers
          </h3>
          
          {/* Search Input */}
          <div className="relative mb-6">
            <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
              <Search className="h-5 w-5 text-gray-400" />
            </div>
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="block w-full pl-10 pr-3 py-2 border border-gray-300 rounded-md leading-5 bg-white placeholder-gray-500 focus:outline-none focus:placeholder-gray-400 focus:ring-1 focus:ring-primary-500 focus:border-primary-500"
              placeholder="Search customers by name..."
            />
          </div>

          {/* Search Results */}
          {customers.length === 0 ? (
            <div className="text-center py-8">
              <Users className="mx-auto h-12 w-12 text-gray-400" />
              <p className="mt-2 text-gray-500">No customers found</p>
              <p className="text-sm text-gray-400">
                Import customers or add them manually to get started
              </p>
            </div>
          ) : (
            <>
              <div className="mb-4 text-sm text-gray-600">
                {searchTerm.trim() === '' 
                  ? `Showing first 20 of ${customers.length} customers`
                  : `Found ${filteredCustomers.length} customers`
                }
              </div>
              
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
                {filteredCustomers.map((customer) => (
                  <div
                    key={customer.id}
                    onClick={() => handleCustomerClick(customer.id)}
                    className="border border-gray-200 rounded-lg p-4 hover:bg-gray-50 cursor-pointer transition-colors duration-200"
                  >
                    <div className="flex items-center justify-between">
                      <div>
                        <h4 className="text-sm font-medium text-gray-900">
                          {customer.firstName} {customer.lastName}
                        </h4>
                        <p className="text-sm text-gray-500">
                          Born: {new Date(customer.birthDate).toLocaleDateString()}
                        </p>
                        <p className="text-xs text-gray-400">
                          ID: {customer.id}
                        </p>
                      </div>
                      <ExternalLink className="h-4 w-4 text-gray-400" />
                    </div>
                  </div>
                ))}
              </div>

              {searchTerm.trim() === '' && customers.length > 20 && (
                <div className="mt-4 text-center">
                  <button
                    onClick={() => navigate('/customers')}
                    className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md text-primary-700 bg-primary-100 hover:bg-primary-200"
                  >
                    View All Customers
                    <ExternalLink className="ml-2 h-4 w-4" />
                  </button>
                </div>
              )}
            </>
          )}
        </div>
      </div>

      {/* Admin Actions */}
      <div className="bg-white shadow rounded-lg">
        <div className="px-4 py-5 sm:p-6">
          <h3 className="text-lg font-medium text-gray-900 mb-4">
            Database Administration
          </h3>
          <div className="rounded-md bg-red-50 p-4">
            <div className="flex">
              <div className="flex-shrink-0">
                <AlertTriangle className="h-5 w-5 text-red-400" />
              </div>
              <div className="ml-3">
                <h3 className="text-sm font-medium text-red-800">
                  Danger Zone
                </h3>
                <div className="mt-2 text-sm text-red-700">
                  <p>
                    This action will permanently delete all customers and readings from the database. 
                    This cannot be undone.
                  </p>
                </div>
                <div className="mt-4">
                  <button
                    onClick={() => setShowClearConfirm(true)}
                    disabled={isClearing}
                    className="inline-flex items-center px-3 py-2 border border-transparent text-sm leading-4 font-medium rounded-md text-white bg-red-600 hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500 disabled:bg-red-400 disabled:cursor-not-allowed"
                  >
                    <Trash2 className="h-4 w-4 mr-2" />
                    {isClearing ? 'Clearing Database...' : 'Clear Database'}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Clear Database Confirmation */}
      {showClearConfirm && (
        <ConfirmDialog
          title="Clear Database"
          message="Are you sure you want to clear the entire database? This will permanently delete all customers and readings. This action cannot be undone."
          onConfirm={handleClearDatabase}
          onCancel={() => setShowClearConfirm(false)}
          confirmText="Clear Database"
          type="danger"
        />
      )}
    </div>
  );
};

export default Dashboard; 