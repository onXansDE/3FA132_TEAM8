import React, { useEffect, useState } from 'react';
import { Users, Activity, TrendingUp, Calendar } from 'lucide-react';
import { customerApi, readingApi } from '../services/api';
import { Customer, Reading } from '../types';
import toast from 'react-hot-toast';

const Dashboard: React.FC = () => {
  const [stats, setStats] = useState({
    totalCustomers: 0,
    totalReadings: 0,
    recentReadings: 0,
    avgReadingsPerCustomer: 0,
  });
  const [recentReadings, setRecentReadings] = useState<Reading[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchDashboardData = async () => {
      try {
        setLoading(true);
        
        // First, fetch all customers
        const customersResponse = await customerApi.getAll();
        const customers = customersResponse.customers || [];

        // Then, fetch readings for each customer
        let allReadings: Reading[] = [];
        
        if (customers.length > 0) {
          const readingPromises = customers.map(async (customer) => {
            try {
              const response = await readingApi.getAll({ customer: customer.id });
              return response.readings || [];
            } catch (error) {
              console.error(`Error fetching readings for customer ${customer.id}:`, error);
              return [];
            }
          });

          const readingArrays = await Promise.all(readingPromises);
          allReadings = readingArrays.flat();
        }

        // Calculate recent readings (last 30 days)
        const thirtyDaysAgo = new Date();
        thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);
        
        const recentReadingsCount = allReadings.filter(reading => 
          new Date(reading.dateOfReading) >= thirtyDaysAgo
        ).length;

        // Get most recent readings for display
        const sortedReadings = allReadings
          .sort((a, b) => new Date(b.dateOfReading).getTime() - new Date(a.dateOfReading).getTime())
          .slice(0, 5);

        setStats({
          totalCustomers: customers.length,
          totalReadings: allReadings.length,
          recentReadings: recentReadingsCount,
          avgReadingsPerCustomer: customers.length > 0 ? Math.round((allReadings.length / customers.length) * 10) / 10 : 0,
        });

        setRecentReadings(sortedReadings);
      } catch (error) {
        console.error('Error fetching dashboard data:', error);
        toast.error('Failed to load dashboard data');
      } finally {
        setLoading(false);
      }
    };

    fetchDashboardData();
  }, []);

  const statCards = [
    {
      name: 'Total Customers',
      value: stats.totalCustomers,
      icon: Users,
      color: 'bg-blue-500',
    },
    {
      name: 'Total Readings',
      value: stats.totalReadings,
      icon: Activity,
      color: 'bg-green-500',
    },
    {
      name: 'Recent Readings',
      value: stats.recentReadings,
      icon: Calendar,
      color: 'bg-yellow-500',
    },
    {
      name: 'Avg Readings/Customer',
      value: stats.avgReadingsPerCustomer,
      icon: TrendingUp,
      color: 'bg-purple-500',
    },
  ];

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
        <p className="ml-4 text-gray-600">Loading customer readings...</p>
      </div>
    );
  }

  return (
    <div>
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
        <p className="mt-1 text-sm text-gray-500">
          Overview of your IDM operations - readings loaded per customer
        </p>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4 mb-8">
        {statCards.map((stat) => {
          const Icon = stat.icon;
          return (
            <div
              key={stat.name}
              className="relative overflow-hidden rounded-lg bg-white px-4 py-5 shadow sm:px-6 sm:py-6"
            >
              <dt>
                <div className={`absolute rounded-md p-3 ${stat.color}`}>
                  <Icon className="h-6 w-6 text-white" />
                </div>
                <p className="ml-16 truncate text-sm font-medium text-gray-500">
                  {stat.name}
                </p>
              </dt>
              <dd className="ml-16 flex items-baseline">
                <p className="text-2xl font-semibold text-gray-900">
                  {stat.value}
                </p>
              </dd>
            </div>
          );
        })}
      </div>

      {/* Recent Readings */}
      <div className="bg-white shadow rounded-lg">
        <div className="px-4 py-5 sm:p-6">
          <h3 className="text-lg font-medium text-gray-900 mb-4">
            Recent Readings (Customer Context)
          </h3>
          {recentReadings.length === 0 ? (
            <div className="text-center py-8">
              <Activity className="mx-auto h-12 w-12 text-gray-400" />
              <p className="mt-2 text-gray-500">No readings found</p>
              <p className="text-sm text-gray-400">Readings are loaded per customer from the API</p>
            </div>
          ) : (
            <div className="overflow-hidden">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Customer
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Meter Type
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Reading
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Date
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {recentReadings.map((reading) => (
                    <tr key={reading.id}>
                      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                        {reading.customer.firstName} {reading.customer.lastName}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        <span className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${
                          reading.kindOfMeter === 'STROM' ? 'bg-yellow-100 text-yellow-800' :
                          reading.kindOfMeter === 'WASSER' ? 'bg-blue-100 text-blue-800' :
                          reading.kindOfMeter === 'HEIZUNG' ? 'bg-red-100 text-red-800' :
                          'bg-gray-100 text-gray-800'
                        }`}>
                          {reading.kindOfMeter}
                        </span>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {reading.meterCount}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {new Date(reading.dateOfReading).toLocaleDateString()}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default Dashboard; 