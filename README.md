This is my attempt of an multi-device integrated application. You have to pair this with the web application, as you can only log in on the Android application, however you need to use the Android application because it is currently the only way to upload / take photos of notes. You can find the web application at (https://github.com/stefanlighezan/SchoolViewWeb). So, what is my app? To sign up, you must drop a Canvas API token, from there, you connect to Firebase and are able to take photos of notes for your courses on Canvas. These photos can be accessed on the web app. They will appear as "Drafts", which means they have no location. You can rename drafts, and move them to different courses, sort of like a miniature play on Google Drive. You can click on individuals courses to see their notes. In these notes, you can click the photo to get the full-scale picture. To get your Canvas Token, you need it too look like this: 
"https://{district name}.instructure.com/api/v1/courses?access_token={access_token}&enrollment_type=student"
To do this, first press Account, then Settings, scroll to New Access Token, set the purpose to anything, and the expiration to never (leave blank), and then click Generate Token. Copy the full length of the token, and now let's work on constructing the full token.
Copy the base name of the website, for example, https://ucscout.instructure.com, without anything else. Then, using the following example (https://{district name}.instructure.com/api/v1/courses?access_token={access_token}&enrollment_type=student), paste the district or Canvas owner name into {district name}, if I were at UC Scout, then it would look like this: 
https://ucscout.instructure.com/api/v1/courses?access_token={access_token}&enrollment_type=student. Now, paste your access token into {access token}. If your code was 123 (which it is NOT), our constructed code would be https://ucscout.instructure.com/api/v1/courses?access_token=123&enrollment_type=student

Below are some NON-WORKING examples:

https://ucscout.instructure.com/api/v1/courses?access_token=Mu4WCA6MHGu4XZQ9MkRV27DDeKGDmwUUMh9hHfkeWUvHBf2Z7rfJt2ykvKktyXWc&enrollment_type=student

https://fusd.instructure.com/api/v1/courses?access_token=Mu4adkmadGu4XZQ9MDDeKGDmwUUM99ad9h9hHfkeWUvHBf2Z7rfJt2ykvKktyXWc&enrollment_type=student

https://sjusd.instructure.com/api/v1/courses?access_token=Mu4WCA6Gu4XZQ9MkRV27DDeK000GDmwUUMh9hHfkeWUvHBf2Z7rfJt2ykvKktyXWc&enrollment_type=student
