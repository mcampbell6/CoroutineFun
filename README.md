# Coroutine Web Sample

### Running Locally
- Install Dependencies `maven install`
- Right click file in `mjc` directory -> `Run 'mjc.{fileName}'`

### BasicCalculator.kt 
A simple application that performs 3 basic tasks and returns the sum of their values using Coroutines 

### AdvancedCalculator.kt
AdvancedCalculator.kt is another implementation of BasicCalculator.kt; however, it simulates querying a quicker cached database for the response of the calculations as well.
- Runs as a web server via [Http4k](https://github.com/http4k/http4k)
- Port: `8010`
- Endpoints: 
    - `/sum`
    - `/sumSync`
- Method:  `GET`
- Query Parameters:
    - `x`: Int - Used in calculation
    - `y`: Int - Used in calculation
    - `z`: Int - Used in calculation
    - `isAuthorized`: Boolean(true) - Returns a 401 if `false`, otherwise, returns the sum of `x`, `y`, and `z`
    - `isCached`: Boolean(false) - Simulates that a values was found in the mocked database
- Example request:
    - `http://localhost:8010/sum?x=30&y=20&z=10&isCached=true&isAuthorized=false`
- Api Client Tool: [Postman](https://www.getpostman.com)