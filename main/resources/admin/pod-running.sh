#!/bin/bash
SECONDS=0

while true; do 
   
   POD_STATUS="$(kubectl get pods --all-namespaces --field-selector=status.phase=Running  | grep $1 )";    
   if [ ! -z "$POD_STATUS" ]
   then 
   break
   fi

   let duration=$SECONDS/60
   if [ $duration -gt 5 ]
   then 
   exit 1
   fi
   
   sleep 10
done   

exit 0