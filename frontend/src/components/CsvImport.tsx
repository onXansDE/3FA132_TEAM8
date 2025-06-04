import React, { useState, useRef } from 'react';
import { Upload, X, Download, CheckCircle } from 'lucide-react';
import { csvImportApi } from '../services/api';
import { Customer, Reading } from '../types';
import toast from 'react-hot-toast';

interface CsvImportProps {
  type: 'customers' | 'readings';
  customers?: Customer[]; // Required for readings import
  onImport: (data: Customer[] | Reading[]) => Promise<void>;
  onCancel: () => void;
}

const CsvImport: React.FC<CsvImportProps> = ({ type, customers = [], onImport, onCancel }) => {
  const [file, setFile] = useState<File | null>(null);
  const [csvContent, setCsvContent] = useState<string>('');
  const [isProcessing, setIsProcessing] = useState(false);
  const [previewLines, setPreviewLines] = useState<string[]>([]);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const customerTemplate = `UUID,Anrede,Vorname,Nachname,Geburtsdatum
ec617965-88b4-4721-8158-ee36c38e4db3,Herr,Pumukel,Kobold,21.02.1962
848c39a1-0cbb-427a-ac6f-a88941943dc8,Herr,André,Schöne,16.02.1928
78dff413-7409-4313-90db-5ec95e969d6d,Frau,Antje,Kittler,12.09.1968
8670e527-3f5e-44cc-ae61-fba80268bd7f,k.A.,Max,Mustermann,15.06.1990`;

  const readingTemplate = `"Kunde";"CUSTOMER_UUID_HERE";
"Zählernummer";"METER_ID_HERE";
;;
"Datum";"Zählerstand";"Kommentar"
01.02.2018;473;
01.04.2018;495;
01.05.2018;505;"Testablesung"`;

  const handleFileUpload = (event: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFile = event.target.files?.[0];
    if (selectedFile && selectedFile.type === 'text/csv') {
      setFile(selectedFile);
      
      const reader = new FileReader();
      reader.onload = (e) => {
        const content = e.target?.result as string;
        setCsvContent(content);
        
        // Show preview of first few lines
        const lines = content.split('\n').slice(0, 10);
        setPreviewLines(lines);
      };
      reader.readAsText(selectedFile);
    } else {
      toast.error('Please select a valid CSV file');
    }
  };

  const handleImport = async () => {
    if (!csvContent) {
      toast.error('No CSV content to import');
      return;
    }

    try {
      setIsProcessing(true);
      
      let result;
      if (type === 'customers') {
        result = await csvImportApi.importCustomers(csvContent);
      } else {
        result = await csvImportApi.importReadings(csvContent);
      }

      toast.success(result.message || `Successfully imported ${result.count} ${type}`);
      
      // Refresh the data in the parent component
      await onImport([]); // Empty array since the backend handles the import
      onCancel();
      
    } catch (error: any) {
      console.error('Import error:', error);
      const errorMessage = error.response?.data?.error || error.message || 'Import failed';
      toast.error(errorMessage);
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

        {/* Format Info */}
        <div className="mb-6 p-4 bg-amber-50 rounded-lg">
          <h4 className="text-sm font-medium text-amber-900 mb-2">Expected Format:</h4>
          {type === 'customers' ? (
            <div className="text-sm text-amber-800">
              <p><strong>Headers:</strong> UUID,Anrede,Vorname,Nachname,Geburtsdatum</p>
              <p><strong>Anrede:</strong> Herr, Frau, or k.A.</p>
              <p><strong>Date format:</strong> DD.MM.YYYY (German format)</p>
            </div>
          ) : (
            <div className="text-sm text-amber-800">
              <p><strong>Special format:</strong> Customer UUID and meter number in first rows</p>
              <p><strong>Readings:</strong> Datum;Zählerstand;Kommentar (semicolon separated)</p>
              <p><strong>Date format:</strong> DD.MM.YYYY or YYYY-MM-DD</p>
            </div>
          )}
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

        {/* File Info & Preview */}
        {file && csvContent && (
          <div className="space-y-4 mb-6">
            <div className="bg-green-50 p-4 rounded-lg">
              <div className="flex items-center">
                <CheckCircle className="h-5 w-5 text-green-500 mr-2" />
                <div>
                  <p className="text-sm font-medium text-green-900">File loaded: {file.name}</p>
                  <p className="text-sm text-green-700">Size: {(file.size / 1024).toFixed(2)} KB</p>
                  <p className="text-sm text-green-700">Lines: {csvContent.split('\n').length}</p>
                </div>
              </div>
            </div>

            {/* Preview */}
            <div className="border rounded-lg">
              <div className="bg-gray-50 px-4 py-2 border-b">
                <h4 className="text-sm font-medium text-gray-900">Preview (first 10 lines)</h4>
              </div>
              <div className="p-4">
                <pre className="text-xs text-gray-600 whitespace-pre-wrap max-h-32 overflow-y-auto">
                  {previewLines.join('\n')}
                </pre>
              </div>
            </div>
          </div>
        )}

        {/* Processing Status */}
        {isProcessing && (
          <div className="text-center py-4 mb-6">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600 mx-auto"></div>
            <p className="mt-2 text-sm text-gray-600">Processing import on server...</p>
          </div>
        )}

        {/* Actions */}
        <div className="flex justify-end space-x-3">
          <button
            onClick={onCancel}
            className="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 hover:bg-gray-50"
          >
            Cancel
          </button>
          <button
            onClick={handleImport}
            disabled={!csvContent || isProcessing}
            className="px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-primary-600 hover:bg-primary-700 disabled:bg-gray-400 disabled:cursor-not-allowed"
          >
            {isProcessing ? 'Importing...' : `Import ${type === 'customers' ? 'Customers' : 'Readings'}`}
          </button>
        </div>
      </div>
    </div>
  );
};

export default CsvImport; 