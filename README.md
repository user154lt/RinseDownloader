# Rinse Downloader

### DISCLAIMER: 
<b>NEITHER ME NOR THIS APP ARE IN ANYWAY AFFILIATED WITH RINSE FM OR ANY OF THEIR CHANNELS.</b>

If you are from Rinse and would like to contact me regarding this app then please do so at user154.apps@gmail.com

### Background: 
I have been listening to Kool since not long after the relaunch as part of Rinse, I've found it pretty awesome because I don't live in London, so aside from the old recording here and there I wasn't able to listen to before. I thought it was really great that you could download shows and listen to them later, however I had just 2 slight niggles with this. The first being that you could only download shows from the last week and occasionally I would forget to download shows that I wanted, secondly the only way
to listen to the downloaded shows was through the Rinse app. I found it bit inconvenient flicking between Rinse and the main app that I use for music depending on what I wanted to listen to. I had thought for a while that I should probably look and find a way to play the downloaded shows with my main music player, but was never really bothered enough to do anything about it. Then one day a show that I had downloaded back in February 2025 (The Harry Shotta show with Erb n Dub) disappeared from my downloads, I was pretty annoyed because it was one of the best sets that I had heard in a long time and I knew that I wouldn't be able to download it again through the app.

This is a fairly simple app written in Kotlin that can get the daily schedules for all Rinse channels from 18/05/21 up to the present day, and then allows the user to download any shows that were downloadable in that time period. The schedules are in json format and can be obtained from the following URL:

https://www.rinse.fm/api/query/v1/schedule/

Simply append the date (In the format YYYY-MM-DD) of the schedule that you wish to obtain to that URL and you will get the schedule for that day. So for example if you wanted the schedule for 09/02/2025 then you would use the URL:

https://www.rinse.fm/api/query/v1/schedule/2025-02-09

If the fileUrl attribute for a show is not null then it is available to download, and the value of this attribute will be the URL for the file. For example:

https://replay.rinse.fm/kool/HarryShotta0902252.mp3