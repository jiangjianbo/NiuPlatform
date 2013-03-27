#!/usr/bin/env groovy
/**
 * License: GPL
 *
 * NiuPlatform is a script for Research & Development Support System (RDSS).
 *
 * User: jjb
 * DateTime: 2013-03-27 21:25
  */



ant = new AntBuilder();


static void main(args){
    if( args.length > 1 && args[0] == "-h" ){
        println '''usageg: NiuPlatform [-h|config]
    -h      show this message
    config  config file or path, default is '.'
                '''
     return;
    }

}



