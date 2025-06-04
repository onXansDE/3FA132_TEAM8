import React from 'react';
import { useForm } from 'react-hook-form';
import { X } from 'lucide-react';
import { Reading, Customer, KindOfMeter } from '../types';

interface ReadingFormProps {
  reading?: Reading;
  customers: Customer[];
  onSubmit: (data: Reading) => void;
  onCancel: () => void;
}

interface FormData {
  customerId: string;
  dateOfReading: string;
  meterId: string;
  kindOfMeter: KindOfMeter;
  meterCount: number;
  substitute: boolean;
  comment?: string;
}

const ReadingForm: React.FC<ReadingFormProps> = ({ reading, customers, onSubmit, onCancel }) => {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormData>({
    defaultValues: reading ? {
      customerId: reading.customer.id,
      dateOfReading: reading.dateOfReading,
      meterId: reading.meterId,
      kindOfMeter: reading.kindOfMeter,
      meterCount: reading.meterCount,
      substitute: reading.substitute,
      comment: reading.comment || '',
    } : {
      substitute: false,
    },
  });

  const onFormSubmit = (data: FormData) => {
    const selectedCustomer = customers.find(c => c.id === data.customerId);
    if (!selectedCustomer) {
      return;
    }

    const readingData: Reading = {
      id: reading?.id || crypto.randomUUID(),
      customer: selectedCustomer,
      dateOfReading: data.dateOfReading,
      meterId: data.meterId,
      kindOfMeter: data.kindOfMeter,
      meterCount: data.meterCount,
      substitute: data.substitute,
      comment: data.comment || undefined,
    };

    onSubmit(readingData);
  };

  return (
    <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
      <div className="relative top-10 mx-auto p-5 border w-full max-w-md shadow-lg rounded-md bg-white">
        <div className="flex justify-between items-center mb-4">
          <h3 className="text-lg font-medium text-gray-900">
            {reading ? 'Edit Reading' : 'Add Reading'}
          </h3>
          <button
            onClick={onCancel}
            className="text-gray-400 hover:text-gray-600"
          >
            <X className="h-6 w-6" />
          </button>
        </div>

        <form onSubmit={handleSubmit(onFormSubmit)} className="space-y-4">
          <div>
            <label htmlFor="customerId" className="block text-sm font-medium text-gray-700">
              Customer
            </label>
            <select
              id="customerId"
              {...register('customerId', { required: 'Customer is required' })}
              className="mt-1 block w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-primary-500 focus:border-primary-500"
            >
              <option value="">Select customer</option>
              {customers.map((customer) => (
                <option key={customer.id} value={customer.id}>
                  {customer.firstName} {customer.lastName}
                </option>
              ))}
            </select>
            {errors.customerId && (
              <p className="mt-1 text-sm text-red-600">{errors.customerId.message}</p>
            )}
          </div>

          <div>
            <label htmlFor="meterId" className="block text-sm font-medium text-gray-700">
              Meter ID
            </label>
            <input
              type="text"
              id="meterId"
              {...register('meterId', { required: 'Meter ID is required' })}
              className="mt-1 block w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-primary-500 focus:border-primary-500"
            />
            {errors.meterId && (
              <p className="mt-1 text-sm text-red-600">{errors.meterId.message}</p>
            )}
          </div>

          <div>
            <label htmlFor="kindOfMeter" className="block text-sm font-medium text-gray-700">
              Meter Type
            </label>
            <select
              id="kindOfMeter"
              {...register('kindOfMeter', { required: 'Meter type is required' })}
              className="mt-1 block w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-primary-500 focus:border-primary-500"
            >
              <option value="">Select meter type</option>
              <option value={KindOfMeter.STROM}>Electricity</option>
              <option value={KindOfMeter.WASSER}>Water</option>
              <option value={KindOfMeter.HEIZUNG}>Heating</option>
              <option value={KindOfMeter.UNBEKANNT}>Unknown</option>
            </select>
            {errors.kindOfMeter && (
              <p className="mt-1 text-sm text-red-600">{errors.kindOfMeter.message}</p>
            )}
          </div>

          <div>
            <label htmlFor="meterCount" className="block text-sm font-medium text-gray-700">
              Meter Reading
            </label>
            <input
              type="number"
              step="0.01"
              id="meterCount"
              {...register('meterCount', { 
                required: 'Meter reading is required',
                valueAsNumber: true,
                min: { value: 0, message: 'Reading must be positive' }
              })}
              className="mt-1 block w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-primary-500 focus:border-primary-500"
            />
            {errors.meterCount && (
              <p className="mt-1 text-sm text-red-600">{errors.meterCount.message}</p>
            )}
          </div>

          <div>
            <label htmlFor="dateOfReading" className="block text-sm font-medium text-gray-700">
              Reading Date
            </label>
            <input
              type="date"
              id="dateOfReading"
              {...register('dateOfReading', { required: 'Reading date is required' })}
              className="mt-1 block w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-primary-500 focus:border-primary-500"
            />
            {errors.dateOfReading && (
              <p className="mt-1 text-sm text-red-600">{errors.dateOfReading.message}</p>
            )}
          </div>

          <div>
            <label htmlFor="comment" className="block text-sm font-medium text-gray-700">
              Comment (Optional)
            </label>
            <textarea
              id="comment"
              rows={3}
              {...register('comment')}
              className="mt-1 block w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-primary-500 focus:border-primary-500"
              placeholder="Add any additional notes..."
            />
          </div>

          <div className="flex items-center">
            <input
              type="checkbox"
              id="substitute"
              {...register('substitute')}
              className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-gray-300 rounded"
            />
            <label htmlFor="substitute" className="ml-2 block text-sm text-gray-900">
              Substitute reading
            </label>
          </div>

          <div className="flex justify-end space-x-3 pt-4">
            <button
              type="button"
              onClick={onCancel}
              className="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-primary-600 hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
            >
              {reading ? 'Update' : 'Create'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default ReadingForm; 