# r2d2
Quantum Compression (Reliably Reconstructable Data Deconstruction)

This is an incredibly experimental algorithm using Torrent hashing and MEQUAVIS layers to deconstruct a file using classical computing in a way that is potentially reconstructable with quantum computers eventually. 

This project is currently an attempt to proof the theory by doing small scalle file reconstruction on the 8-bit and 16-bit level, with the intention of doing 32-bit and 64-bit reconstruction using cloud computing (distributed systems)

In it's current form this will not compress your files and can actually make them larger and it does not retain your data in a way that I can promise that it can be restored currently.

The intention is to get it to that point though and that can be hsown mathematically up to a certain bit level.

However with quantum computers that can do classical computations we could theoretically use this at absurdly high bit levels such as 2048 bit, which would yield incredible levels of compressions!

If this was worked out to it's full extent we could theoretically store the entire Earth's known data in a few gigabytes that a super computer could then inflate what it needs as it needs it.

This functionality would prove invaluable for space ships in the near future when a team may not have active live access to the internet for research and such. They could just inflate the archived internet they needed.

This is of course a long term and lofty goal.

Current algorithm makes a tiered layering of 8-bit and up torrent files that are then compressed into a single archive with a few extra files that record bit data for future inflation. This part is not completed as another layer of tracking is required.

This is a pet project or a side project. Not commercially available at this time or for any time in the foreseeable future.

Jokingly referred to as QNC (Quantum NanoCompression) or R2D2

A compiled and working version of R2D2 is available for demonstration with the nanocheeze.exe app for the MEQUAVIS Control Panel GUI

https://xtdevelopment.net/nanocheeze.exe

Warning: app requires 32bit JAVA SDK! (Not the JRE)

You can run the included JAR file above if you want but the corresponding torrent verifier exe will only run if you launch this from the nanocheeze.exe application.

It is setup to compress files all the way up to the ridiculous level in theory you could make time capsules of your files in the hopes that the future can use these compressed files to reconstruct your data.

ie. You could encrypt several 100 gigabytes of home videos into a single half gigabyte, put it on a cd or usb stick and then bury it in a time capsule. In theory there is enough data in there tiered torrent files to reproduce the data given enough processing power and advancement in computer technology and algorthms. But by the time they find this they will probably have developed a different version of this method so reconstructing your time capsule data might prove non-trivial for the future.

Images of current process in action:

https://raw.githubusercontent.com/cybershrapnel/r2d2/master/images/8bit.png
https://raw.githubusercontent.com/cybershrapnel/r2d2/master/images/warning.png
https://raw.githubusercontent.com/cybershrapnel/r2d2/master/images/warning2.png
https://raw.githubusercontent.com/cybershrapnel/r2d2/master/images/level.png
https://raw.githubusercontent.com/cybershrapnel/r2d2/master/images/verify.png
https://raw.githubusercontent.com/cybershrapnel/r2d2/master/images/verified.png
