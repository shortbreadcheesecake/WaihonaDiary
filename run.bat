::[Bat To Exe Converter]
::
::YAwzoRdxOk+EWAjk
::fBw5plQjdCyDJGyX8VAjFBpBWgGRAEi7FbYd7O3L/eWNp04JR94SfYvS24CcIeMY71fYZoQu6ndbkcUwHh5LcQe5bQonlW1Nu1iIMsOVshvzS0uC41kMGnZngnfDsCI4b90mk8AMsw==
::YAwzuBVtJxjWCl3EqQJgSA==
::ZR4luwNxJguZRRnk
::Yhs/ulQjdFy5
::cxAkpRVqdFKZSzk=
::cBs/ulQjdFy5
::ZR41oxFsdFKZSDk=
::eBoioBt6dFKZSDk=
::cRo6pxp7LAbNWATEpCI=
::egkzugNsPRvcWATEpCI=
::dAsiuh18IRvcCxnZtBJQ
::cRYluBh/LU+EWAnk
::YxY4rhs+aU+JeA==
::cxY6rQJ7JhzQF1fEqQJQ
::ZQ05rAF9IBncCkqN+0xwdVs0
::ZQ05rAF9IAHYFVzEqQJQ
::eg0/rx1wNQPfEVWB+kM9LVsJDGQ=
::fBEirQZwNQPfEVWB+kM9LVsJDGQ=
::cRolqwZ3JBvQF1fEqQJQ
::dhA7uBVwLU+EWDk=
::YQ03rBFzNR3SWATElA==
::dhAmsQZ3MwfNWATElA==
::ZQ0/vhVqMQ3MEVWAtB9wSA==
::Zg8zqx1/OA3MEVWAtB9wSA==
::dhA7pRFwIByZRRnk
::YB416Ek+ZG8=
::
::
::978f952a14a936cc963da21a135fa983
@echo off
setlocal enabledelayedexpansion

:: Проверяем, что Maven установлен
where mvn >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo Maven не найден в PATH. Убедитесь, что Maven установлен и добавлен в PATH.
    pause
    exit /b 1
)

:: Проверяем версию Java
for /f "tokens=3" %%a in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set "java_version=%%a"
    set "java_version=!java_version:"=!"
    for /f "tokens=1,2 delims=._" %%b in ("!java_version!") do (
        set "java_major=%%b"
        set "java_minor=%%c"
    )
)

if not defined java_major (
    echo Java не найдена. Установите JDK 17 или новее.
    pause
    exit /b 1
)

if !java_major! LSS 17 (
    echo Обнаружена Java !java_major!.!java_minor!. Требуется Java 17 или новее.
    pause
    exit /b 1
)

:: Запускаем приложение
echo Запуск приложения с Java !java_major!...
mvn clean javafx:run

pause
