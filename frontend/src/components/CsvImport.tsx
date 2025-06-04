import React, { useState, useRef } from 'react';
import { Upload, X, Download, AlertCircle, CheckCircle } from 'lucide-react';
import Papa from 'papaparse';
import { Customer, Reading, Gender, KindOfMeter } from '../types';
import toast from 'react-hot-toast';

interface CsvImportProps {
  type: 'customers' | 'readings';
  customers?: Customer[]; // Required for readings import
  onImport: (data: Customer[] | Reading[]) => Promise<void>;
  onCancel: () => void;
}

interface ParsedCustomer {
  firstName: string;
  lastName: string;
  birthDate: string;
  gender: string;
  errors: string[];
}

interface ParsedReading {
  customerName: string;
  dateOfReading: string;
  meterId: string;
  kindOfMeter: string;
  meterCount: string;
  substitute: string;
  comment?: string;
  errors: string[];
}

const CsvImport: React.FC<CsvImportProps> = ({ type, customers = [], onImport, onCancel }) => {
  const [file, setFile] = useState<File | null>(null);
  const [parsedData, setParsedData] = useState<(ParsedCustomer | ParsedReading)[]>([]);
  const [isProcessing, setIsProcessing] = useState(false);
  const [validData, setValidData] = useState<(Customer | Reading)[]>([]);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const customerTemplate = `firstName,lastName,birthDate,gender
John,Doe,1990-01-15,M
Jane,Smith,1985-07-22,W
Alex,Johnson,1992-03-10,D`;

  const readingTemplate = `customerName,dateOfReading,meterId,kindOfMeter,meterCount,substitute,comment
John Doe,2024-01-15,METER001,STROM,150.5,false,Regular reading
Jane Smith,2024-01-15,METER002,WASSER,75.2,true,Estimated reading`;

  const handleFileUpload = (event: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFile = event.target.files?.[0];
    if (selectedFile && selectedFile.type === 'text/csv') {
      setFile(selectedFile);
      parseFile(selectedFile);
    } else {
      toast.error('Please select a valid CSV file');
    }
  };

  const parseFile = (file: File) => {
    setIsProcessing(true);
    
    Papa.parse(file, {
      header: true,
      skipEmptyLines: true,
      complete: (results) => {
        const data = results.data as any[];
        if (type === 'customers') {
          processCustomers(data);
        } else {
          processReadings(data);
        }
        setIsProcessing(false);
      },
      error: (error) => {
        toast.error(`CSV parsing error: ${error.message}`);
        setIsProcessing(false);
      }
    });
  };

  const processCustomers = (data: any[]) => {
    const processed: ParsedCustomer[] = data.map((row, index) => {
      const errors: string[] = [];
      
      if (!row.firstName?.trim()) errors.push('First name is required');
      if (!row.lastName?.trim()) errors.push('Last name is required');
      if (!row.birthDate?.trim()) errors.push('Birth date is required');
      if (!row.gender?.trim()) errors.push('Gender is required');
      
      if (row.birthDate && !isValidDate(row.birthDate)) {
        errors.push('Invalid date format (use YYYY-MM-DD)');
      }
      
      if (row.gender && !['M', 'W', 'D', 'U'].includes(row.gender.toUpperCase())) {
        errors.push('Gender must be M, W, D, or U');
      }

      return {
        firstName: row.firstName?.trim() || '',
        lastName: row.lastName?.trim() || '',
        birthDate: row.birthDate?.trim() || '',
        gender: row.gender?.toUpperCase().trim() || '',
        errors
      };
    });

    setParsedData(processed);
    
    const valid: Customer[] = processed
      .filter(item => item.errors.length === 0)
      .map(item => ({
        id: crypto.randomUUID(),
        firstName: item.firstName,
        lastName: item.lastName,
        birthDate: item.birthDate,
        gender: item.gender as Gender
      }));
    
    setValidData(valid);
  };

  const processReadings = (data: any[]) => {
    const processed: ParsedReading[] = data.map((row, index) => {
      const errors: string[] = [];
      
      if (!row.customerName?.trim()) errors.push('Customer name is required');
      if (!row.dateOfReading?.trim()) errors.push('Date of reading is required');
      if (!row.meterId?.trim()) errors.push('Meter ID is required');
      if (!row.kindOfMeter?.trim()) errors.push('Kind of meter is required');
      if (!row.meterCount?.trim()) errors.push('Meter count is required');
      if (row.substitute === undefined || row.substitute === null || row.substitute === '') {
        errors.push('Substitute field is required');
      }
      
      if (row.dateOfReading && !isValidDate(row.dateOfReading)) {
        errors.push('Invalid date format (use YYYY-MM-DD)');
      }
      
      if (row.kindOfMeter && !['STROM', 'WASSER', 'HEIZUNG', 'UNBEKANNT'].includes(row.kindOfMeter.toUpperCase())) {
        errors.push('Kind of meter must be STROM, WASSER, HEIZUNG, or UNBEKANNT');
      }
      
      if (row.meterCount && (isNaN(parseFloat(row.meterCount)) || parseFloat(row.meterCount) < 0)) {
        errors.push('Meter count must be a positive number');
      }
      
      const customer = customers.find(c => 
        `${c.firstName} ${c.lastName}`.toLowerCase() === row.customerName?.toLowerCase().trim()
      );
      
      if (row.customerName && !customer) {
        errors.push('Customer not found (check spelling and case)');
      }

      return {
        customerName: row.customerName?.trim() || '',
        dateOfReading: row.dateOfReading?.trim() || '',
        meterId: row.meterId?.trim() || '',
        kindOfMeter: row.kindOfMeter?.toUpperCase().trim() || '',
        meterCount: row.meterCount?.trim() || '',
        substitute: row.substitute?.toString().toLowerCase().trim() || '',
        comment: row.comment?.trim() || undefined,
        errors
      };
    });

    setParsedData(processed);
    
    const valid: Reading[] = processed
      .filter(item => item.errors.length === 0)
      .map(item => {
        const customer = customers.find(c => 
          `${c.firstName} ${c.lastName}`.toLowerCase() === item.customerName.toLowerCase()
        )!;
        
        return {
          id: crypto.randomUUID(),
          customer,
          dateOfReading: item.dateOfReading,
          meterId: item.meterId,
          kindOfMeter: item.kindOfMeter as KindOfMeter,
          meterCount: parseFloat(item.meterCount),
          substitute: item.substitute === 'true' || item.substitute === '1',
          comment: item.comment
        };
      });
    
    setValidData(valid);
  };

  const isValidDate = (dateString: string): boolean => {
    const date = new Date(dateString);
    return date instanceof Date && !isNaN(date.getTime()) && dateString.match(/^\d{4}-\d{2}-\d{2}$/);
  };

  const handleImport = async () => {
    if (validData.length === 0) {
      toast.error('No valid data to import');
      return;
    }

    try {
      setIsProcessing(true);
      await onImport(validData);
      toast.success(`Successfully imported ${validData.length} ${type}`);
      onCancel();
    } catch (error) {
      toast.error(`Import failed: ${error}`);
    } finally {
      setIsProcessing(false);
    }
  };

  const downloadTemplate = () => {
    const template = type === 'customers' ? customerTemplate : readingTemplate;
    const blob = new Blob([template], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `${type}_template.csv`;
    link.click();
    window.URL.revokeObjectURL(url);
  };

  const errorCount = parsedData.filter(item => item.errors.length > 0).length;
  const validCount = validData.length;

  return (
    <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
      <div className="relative top-10 mx-auto p-6 border w-full max-w-4xl shadow-lg rounded-md bg-white">
        <div className="flex justify-between items-center mb-6">
          <h3 className="text-lg font-medium text-gray-900">
            Import {type === 'customers' ? 'Customers' : 'Readings'} from CSV
          </h3>
          <button onClick={onCancel} className="text-gray-400 hover:text-gray-600">
            <X className="h-6 w-6" />
          </button>
        </div>

        {/* Template Download */}
        <div className="mb-6 p-4 bg-blue-50 rounded-lg">
          <div className="flex items-center justify-between">
            <div>
              <h4 className="text-sm font-medium text-blue-900">Need a template?</h4>
              <p className="text-sm text-blue-700">Download the CSV template with the correct format</p>
            </div>
            <button
              onClick={downloadTemplate}
              className="inline-flex items-center px-3 py-2 border border-blue-300 rounded-md text-sm font-medium text-blue-700 bg-white hover:bg-blue-50"
            >
              <Download className="h-4 w-4 mr-2" />
              Download Template
            </button>
          </div>
        </div>

        {/* File Upload */}
        <div className="mb-6">
          <div className="mt-1 flex justify-center px-6 pt-5 pb-6 border-2 border-gray-300 border-dashed rounded-md">
            <div className="space-y-1 text-center">
              <Upload className="mx-auto h-12 w-12 text-gray-400" />
              <div className="flex text-sm text-gray-600">
                <label htmlFor="file-upload" className="relative cursor-pointer bg-white rounded-md font-medium text-primary-600 hover:text-primary-500 focus-within:outline-none focus-within:ring-2 focus-within:ring-offset-2 focus-within:ring-primary-500">
                  <span>Upload a CSV file</span>
                  <input
                    ref={fileInputRef}
                    id="file-upload"
                    name="file-upload"
                    type="file"
                    accept=".csv"
                    className="sr-only"
                    onChange={handleFileUpload}
                  />
                </label>
                <p className="pl-1">or drag and drop</p>
              </div>
              <p className="text-xs text-gray-500">CSV files only</p>
            </div>
          </div>
        </div>

        {/* Processing/Results */}
        {isProcessing && (
          <div className="text-center py-4">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600 mx-auto"></div>
            <p className="mt-2 text-sm text-gray-600">Processing file...</p>
          </div>
        )}

        {file && parsedData.length > 0 && !isProcessing && (
          <div className="space-y-4">
            {/* Summary */}
            <div className="grid grid-cols-3 gap-4">
              <div className="text-center p-4 bg-gray-50 rounded-lg">
                <div className="text-2xl font-bold text-gray-900">{parsedData.length}</div>
                <div className="text-sm text-gray-600">Total Rows</div>
              </div>
              <div className="text-center p-4 bg-green-50 rounded-lg">
                <div className="text-2xl font-bold text-green-600">{validCount}</div>
                <div className="text-sm text-green-600">Valid</div>
              </div>
              <div className="text-center p-4 bg-red-50 rounded-lg">
                <div className="text-2xl font-bold text-red-600">{errorCount}</div>
                <div className="text-sm text-red-600">Errors</div>
              </div>
            </div>

            {/* Preview Table */}
            <div className="max-h-64 overflow-y-auto border rounded-lg">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
                    {type === 'customers' ? (
                      <>
                        <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">First Name</th>
                        <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">Last Name</th>
                        <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">Birth Date</th>
                        <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">Gender</th>
                      </>
                    ) : (
                      <>
                        <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">Customer</th>
                        <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">Date</th>
                        <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">Meter ID</th>
                        <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">Type</th>
                        <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">Count</th>
                      </>
                    )}
                    <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">Errors</th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {parsedData.slice(0, 10).map((row, index) => (
                    <tr key={index} className={row.errors.length > 0 ? 'bg-red-50' : 'bg-green-50'}>
                      <td className="px-3 py-2 whitespace-nowrap">
                        {row.errors.length > 0 ? (
                          <AlertCircle className="h-4 w-4 text-red-500" />
                        ) : (
                          <CheckCircle className="h-4 w-4 text-green-500" />
                        )}
                      </td>
                      {type === 'customers' ? (
                        <>
                          <td className="px-3 py-2 text-sm text-gray-900">{(row as ParsedCustomer).firstName}</td>
                          <td className="px-3 py-2 text-sm text-gray-900">{(row as ParsedCustomer).lastName}</td>
                          <td className="px-3 py-2 text-sm text-gray-900">{(row as ParsedCustomer).birthDate}</td>
                          <td className="px-3 py-2 text-sm text-gray-900">{(row as ParsedCustomer).gender}</td>
                        </>
                      ) : (
                        <>
                          <td className="px-3 py-2 text-sm text-gray-900">{(row as ParsedReading).customerName}</td>
                          <td className="px-3 py-2 text-sm text-gray-900">{(row as ParsedReading).dateOfReading}</td>
                          <td className="px-3 py-2 text-sm text-gray-900">{(row as ParsedReading).meterId}</td>
                          <td className="px-3 py-2 text-sm text-gray-900">{(row as ParsedReading).kindOfMeter}</td>
                          <td className="px-3 py-2 text-sm text-gray-900">{(row as ParsedReading).meterCount}</td>
                        </>
                      )}
                      <td className="px-3 py-2 text-sm text-red-600">
                        {row.errors.join(', ')}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {parsedData.length > 10 && (
                <div className="px-3 py-2 text-sm text-gray-500 text-center border-t">
                  ... and {parsedData.length - 10} more rows
                </div>
              )}
            </div>

            {/* Actions */}
            <div className="flex justify-end space-x-3 pt-4">
              <button
                onClick={onCancel}
                className="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 hover:bg-gray-50"
              >
                Cancel
              </button>
              <button
                onClick={handleImport}
                disabled={validCount === 0 || isProcessing}
                className="px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-primary-600 hover:bg-primary-700 disabled:bg-gray-400 disabled:cursor-not-allowed"
              >
                Import {validCount} {type === 'customers' ? 'Customers' : 'Readings'}
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default CsvImport; 