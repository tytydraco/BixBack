# BixBack
An extremely lightweight Bixby remapper (8.0 compatible) that goes "back".

BixBack functions by periodically scanning for a certain log in the logcat. Once the log is discovered, 
it checks to make sure it occured in a proper time frame, and then executes the BACK global action.
