#!/bin/bash
SECONDS=0

while true; do 
   HTTP_STATUS="$(curl -ILk --silent $1 | grep 200 )";    
   if [ ! -z "$HTTP_STATUS" ]
   then 
   break
   fi

   let duration=$SECONDS/60
   if [ $duration -gt 5 ]
   then 
   exit 1
   fi
   
   sleep 20
done   

exit 0