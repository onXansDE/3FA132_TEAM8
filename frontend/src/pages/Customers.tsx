import React, { useEffect, useState } from 'react';
import { Plus, Edit, Trash2, Search, Upload } from 'lucide-react';
import { customerApi } from '../services/api';
import { Customer, Gender, Reading } from '../types';
import toast from 'react-hot-toast';
import CustomerForm from '../components/CustomerForm';
import ConfirmDialog from '../components/ConfirmDialog';
import CsvImport from '../components/CsvImport';
import { useNavigate } from 'react-router-dom';

const Customers: React.FC = () => {
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [editingCustomer, setEditingCustomer] = useState<Customer | null>(null);
  const [deletingCustomer, setDeletingCustomer] = useState<Customer | null>(null);
  const [showCsvImport, setShowCsvImport] = useState(false);

  const navigate = useNavigate();

  useEffect(() => {
    fetchCustomers();
  }, []);

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

  const handleCreateCustomer = async (customerData: Customer) => {
    try {
      await customerApi.create(customerData);
      toast.success('Customer created successfully');
      setShowForm(false);
      fetchCustomers();
    } catch (error) {
      console.error('Error creating customer:', error);
      toast.error('Failed to create customer');
    }
  };

  const handleUpdateCustomer = async (customerData: Customer) => {
    try {
      await customerApi.update(customerData);
      toast.success('Customer updated successfully');
      setEditingCustomer(null);
      fetchCustomers();
    } catch (error) {
      console.error('Error updating customer:', error);
      toast.error('Failed to update customer');
    }
  };

  const handleDeleteCustomer = async (customer: Customer) => {
    try {
      await customerApi.delete(customer.id);
      toast.success('Customer deleted successfully');
      setDeletingCustomer(null);
      fetchCustomers();
    } catch (error) {
      console.error('Error deleting customer:', error);
      toast.error('Failed to delete customer');
    }
  };

  const handleBulkImport = async (importData: Customer[] | Reading[]) => {
    // The backend handles the import, so we just need to refresh the data
    fetchCustomers();
  };

  const handleCustomerClick = (customerId: string) => {
    navigate(`/readings?customer=${customerId}`);
  };

  const filteredCustomers = customers.filter(customer =>
    customer.firstName.toLowerCase().includes(searchTerm.toLowerCase()) ||
    customer.lastName.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const getGenderLabel = (gender: Gender) => {
    switch (gender) {
      case Gender.M: return 'Male';
      case Gender.W: return 'Female';
      case Gender.D: return 'Diverse';
      case Gender.U: return 'Unknown';
      default: return gender;
    }
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
          <h1 className="text-2xl font-bold text-gray-900">Customers</h1>
          <p className="mt-1 text-sm text-gray-500">
            Manage customer information and profiles
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
            Add Customer
          </button>
        </div>
      </div>

      {/* Search */}
      <div className="mb-6">
        <div className="relative">
          <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
            <Search className="h-5 w-5 text-gray-400" />
          </div>
          <input
            type="text"
            placeholder="Search customers..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="block w-full pl-10 pr-3 py-2 border border-gray-300 rounded-md leading-5 bg-white placeholder-gray-500 focus:outline-none focus:placeholder-gray-400 focus:ring-1 focus:ring-primary-500 focus:border-primary-500"
          />
        </div>
      </div>

      {/* Customers Table */}
      <div className="bg-white shadow overflow-hidden sm:rounded-md">
        {filteredCustomers.length === 0 ? (
          <div className="text-center py-12">
            <p className="text-gray-500">No customers found</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Name
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Birth Date
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Gender
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {filteredCustomers.map((customer) => (
                  <tr 
                    key={customer.id} 
                    className="hover:bg-gray-50 cursor-pointer"
                    onClick={() => handleCustomerClick(customer.id)}
                  >
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm font-medium text-gray-900">
                        {customer.firstName} {customer.lastName}
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {new Date(customer.birthDate).toLocaleDateString()}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {getGenderLabel(customer.gender)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                      <div className="flex space-x-2" onClick={(e) => e.stopPropagation()}>
                        <button
                          onClick={() => setEditingCustomer(customer)}
                          className="text-primary-600 hover:text-primary-900"
                        >
                          <Edit className="h-4 w-4" />
                        </button>
                        <button
                          onClick={() => setDeletingCustomer(customer)}
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

      {/* Create Customer Form */}
      {showForm && (
        <CustomerForm
          onSubmit={handleCreateCustomer}
          onCancel={() => setShowForm(false)}
        />
      )}

      {/* Edit Customer Form */}
      {editingCustomer && (
        <CustomerForm
          customer={editingCustomer}
          onSubmit={handleUpdateCustomer}
          onCancel={() => setEditingCustomer(null)}
        />
      )}

      {/* Delete Confirmation */}
      {deletingCustomer && (
        <ConfirmDialog
          title="Delete Customer"
          message={`Are you sure you want to delete ${deletingCustomer.firstName} ${deletingCustomer.lastName}? This action cannot be undone.`}
          onConfirm={() => handleDeleteCustomer(deletingCustomer)}
          onCancel={() => setDeletingCustomer(null)}
        />
      )}

      {/* CSV Import */}
      {showCsvImport && (
        <CsvImport
          type="customers"
          onImport={handleBulkImport}
          onCancel={() => setShowCsvImport(false)}
        />
      )}
    </div>
  );
};

export default Customers; 