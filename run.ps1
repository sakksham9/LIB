# PageTurn Library - Run Script
$env:DB_URL = "jdbc:postgresql://aws-0-ap-northeast-1.pooler.supabase.com:5432/postgres?sslmode=require"
$env:DB_USERNAME = "postgres.shkzvcyjmllpwykwgpwl"
$env:DB_PASSWORD = "@S19a1k11s19"

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host " Starting PageTurn Backend (Spring Boot)" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Database URL: $env:DB_URL" -ForegroundColor DarkGray
Write-Host "Application URL: http://localhost:8080" -ForegroundColor Green
Write-Host "API Endpoint:    http://localhost:8080/api/books" -ForegroundColor Cyan

mvn spring-boot:run
