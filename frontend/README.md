# IDM Operations Frontend

A modern React TypeScript frontend for the IDM (Identity Management) Operations API. This application provides a comprehensive interface for managing customers and meter readings with a beautiful, responsive design.

## Features

### 🏠 Dashboard
- Overview statistics (total customers, readings, recent activity)
- Recent readings table with color-coded meter types
- Real-time data visualization

### 👥 Customer Management
- Complete CRUD operations for customers
- Search and filter functionality
- Form validation with error handling
- Gender selection (Male, Female, Diverse, Unknown)
- Birth date management

### 📊 Meter Readings
- Full CRUD operations for meter readings
- Advanced filtering by:
  - Customer
  - Meter type (Electricity, Water, Heating, Unknown)
  - Date range
- Search functionality
- Substitute reading tracking
- Comments and notes

### 🎨 UI/UX Features
- Responsive design (mobile-first)
- Modern Tailwind CSS styling
- Loading states and error handling
- Toast notifications for user feedback
- Modal forms for data entry
- Confirmation dialogs for destructive actions

## Technology Stack

- **React 18** - Modern React with hooks
- **TypeScript** - Type-safe development
- **Tailwind CSS** - Utility-first CSS framework
- **React Router** - Client-side routing
- **React Hook Form** - Form handling and validation
- **Axios** - HTTP client for API communication
- **React Hot Toast** - Toast notifications
- **Lucide React** - Beautiful icons

## Getting Started

### Prerequisites

- Node.js 16+ and npm
- The IDM Operations Java API running on `http://localhost:8080`

### Installation

1. Navigate to the frontend directory:
   ```bash
   cd frontend
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. Start the development server:
   ```bash
   npm start
   ```

4. Open your browser and navigate to `http://localhost:3000`

### Available Scripts

- `npm start` - Start development server
- `npm build` - Build for production
- `npm test` - Run tests
- `npm run eject` - Eject from Create React App (not recommended)

## API Integration

The frontend communicates with the Java API through a proxy configuration. All API requests are automatically proxied to `http://localhost:8080` during development.

### API Endpoints Used

- `GET /rest/customers` - Get all customers
- `GET /rest/customers/{id}` - Get customer by ID
- `POST /rest/customers/create` - Create new customer
- `PUT /rest/customers` - Update customer
- `DELETE /rest/customers/{id}` - Delete customer

- `GET /rest/readings` - Get all readings (with optional filters)
- `GET /rest/readings/{id}` - Get reading by ID
- `POST /rest/readings` - Create new reading
- `PUT /rest/readings` - Update reading
- `DELETE /rest/readings/{id}` - Delete reading

## Project Structure

```
frontend/
├── public/
│   └── index.html
├── src/
│   ├── components/
│   │   ├── Layout.tsx          # Main layout with navigation
│   │   ├── CustomerForm.tsx    # Customer create/edit form
│   │   ├── ReadingForm.tsx     # Reading create/edit form
│   │   └── ConfirmDialog.tsx   # Reusable confirmation dialog
│   ├── pages/
│   │   ├── Dashboard.tsx       # Dashboard with statistics
│   │   ├── Customers.tsx       # Customer management page
│   │   └── Readings.tsx        # Reading management page
│   ├── services/
│   │   └── api.ts             # API service layer
│   ├── types/
│   │   └── index.ts           # TypeScript type definitions
│   ├── App.tsx                # Main app component
│   ├── index.tsx              # App entry point
│   └── index.css              # Global styles
├── package.json
├── tailwind.config.js
└── README.md
```

## Data Models

### Customer
```typescript
interface Customer {
  id: string;
  firstName: string;
  lastName: string;
  birthDate: string; // ISO date string
  gender: 'M' | 'W' | 'D' | 'U'; // Male, Female, Diverse, Unknown
}
```

### Reading
```typescript
interface Reading {
  id: string;
  customer: Customer;
  dateOfReading: string; // ISO date string
  meterId: string;
  kindOfMeter: 'STROM' | 'WASSER' | 'HEIZUNG' | 'UNBEKANNT';
  meterCount: number;
  substitute: boolean;
  comment?: string;
}
```

## Development

### Code Style
- TypeScript strict mode enabled
- ESLint configuration from Create React App
- Consistent component structure with props interfaces
- Error handling with try-catch blocks
- Loading states for better UX

### State Management
- React hooks for local state
- No external state management library (Redux, Zustand) needed
- API calls handled in page components
- Form state managed by React Hook Form

## Building for Production

1. Build the application:
   ```bash
   npm run build
   ```

2. The build artifacts will be in the `build/` directory

3. Serve the static files with any web server

## Browser Support

- Chrome (latest)
- Firefox (latest)
- Safari (latest)
- Edge (latest)

## Contributing

1. Follow the existing code style
2. Add TypeScript types for new features
3. Include error handling for API calls
4. Test responsive design on mobile devices
5. Update this README for new features 