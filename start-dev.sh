#!/bin/bash

# IDM Operations Development Startup Script
# This script starts both the Java backend and React frontend

echo "🚀 Starting IDM Operations Development Environment..."

# Function to cleanup background processes on exit
cleanup() {
    echo "🛑 Shutting down services..."
    kill $BACKEND_PID $FRONTEND_PID 2>/dev/null
    exit 0
}

# Set up signal handlers
trap cleanup SIGINT SIGTERM

# Start the Java backend
echo "📊 Starting Java backend API..."
mvn exec:java &
BACKEND_PID=$!

# Wait a moment for backend to start
sleep 5

# Start the React frontend
echo "🎨 Starting React frontend..."
cd frontend
npm start &
FRONTEND_PID=$!
cd ..

echo "✅ Development environment started!"
echo "📊 Backend API: http://localhost:8080"
echo "🎨 Frontend App: http://localhost:3000"
echo ""
echo "Press Ctrl+C to stop all services"

# Wait for both processes
wait $BACKEND_PID $FRONTEND_PID 