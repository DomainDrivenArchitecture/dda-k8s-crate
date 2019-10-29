#!/bin/bash
SECONDS=0

while true; do 

   POD_STATUS="$(kubectl get pods --all-namespaces --field-selector=status.phase=Running  | grep $1 )";    
   if [ ! -z "$POD_STATUS" ]
   then 
   # pod = ready is not enough
   sleep $4
   break
   fi

   let duration=$SECONDS/60
   if [ "$duration" -ge "$2" ]
   then 
   exit 1
   fi

   sleep $3
done   

exit 0 
