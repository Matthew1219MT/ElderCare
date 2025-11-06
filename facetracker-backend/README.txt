How to Run the Backend

A. Locally
1. Open folder backend-facetracker
2. Run command 
python application.py
3. Insert the link in the local.properties with 
FACETRACKER_API_BASE_URL=<link here>

NOTE: If you’re using emulator, change localhost with http://10.0.2.2:5000

B. Using Ngrok
1. Open folder backend-facetracker
2. Run command 
python application.py
4. Run command
ngrok http 5000
5. Insert the link provided by ngrok
FACETRACKER_API_BASE_URL=<link here>

C. Using AWS
1. Compress the application.py, requirements.txt, and runtime.txt into a zip (or you can use facetracker-api.zip)
2. Open AWS Beanstalk
3. Configure as Python Application
4. Upload the zip
5. Use load balancer configuration
6. Deploy the environment
7. Pass the load balancer DNS name as a CNAME of your domain/subdomain
8. Config the SSL for that domain/subdomain using Amazon Certificate Manager
9. Use the domain/subdomain inside the local.properties
FACETRACKER_API_BASE_URL=<link here>