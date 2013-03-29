#!/usr/bin/env groovy
/**
 * License: GPL
 *
 * NiuPlatform is a script for Research & Development Support System (RDSS).
 *
 * User: jjb
 * DateTime: 2013-03-27 21:25
  */


// process command line
if( args.length == 0 || args[0] == "-h" ){
    println '''usageg: NiuPlatform [-h|config]
    -h      show this message
    config  config file or path, default is '.'
            '''
    return;
}

// read config
var platform_base = "";

// initialize
ant = new AntBuilder();

// create structure
ant.mkdir(dir:"${platform_base}/program");
ant.mkdir(dir:"${platform_base}/data");
ant.mkdir(dir:"${platform_base}/tools/packages");

install_joomla();
install_drupal();

function install_joomla(){
    require_apache();
    require_php();
    require_mysql();

    download_joomla();
    extract_joomla();
    config_joomla();
}



function download_apache(){

}

function install_apache(){

}








