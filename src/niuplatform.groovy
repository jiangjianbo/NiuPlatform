#!/usr/bin/env groovy

import groovy.transform.Field
import static Globals.*
/**
 * License: GPL
 *
 * NiuPlatform is a script for Research & Development Support System (RDSS).
 *
 * User: jjb
 * DateTime: 2013-03-27 21:25
  */

class Globals{

    // read config
    static String platform_base = new File("../output").canonicalPath.replace('\\','/');
    static String program_dir = "${platform_base}/program"
    static String data_dir = "${platform_base}/data"
    static String package_dir = "${platform_base}/tools/packages"

    static AntBuilder ant = new AntBuilder();

    static Properties properties = new Properties();

    static apache = new ApacheOp();
    static php = new PhpOp();
    static mysql = new MysqlOp();
    static joomla = new JoomlaOp();
    // static drupal = new DrupalOp();


//////////////////////////////////////////////////////////////
////  help functions
//////////////////////////////////////////////////////////////

    static boolean _dir_exist(dir){
        def f = new File(dir);
        return f.exists() && f.isDirectory();
    }

    static boolean _file_exist(file){
        def f = new File(file);
        return f.exists() && f.isFile();
    }

    static void _adjust_level(dir,String file,String wantpos){
        if( _file_exist("${dir}/${wantpos}")) return
        File found = null;
        def check = {File f ->
            if( f.isFile() && f.name == file )
                found = f;
            else if( f.isDirectory() ){
                if( new File(f, wantpos).exists() )
                    found = new File(f, wantpos)
            }
        }
        File d = new File(dir);
        check(d)
        d.eachFileRecurse(check)
        if( found != null ){
            String rel = found.canonicalPath[d.canonicalPath.size() .. -1]
            if( rel.replaceAll("\\\\","/").toLowerCase().endsWith(wantpos.toLowerCase()) ){
                if( rel.size() != wantpos.size() ){
                    String mid = rel[0 .. (rel.size() - wantpos.size() -1)]
                    mid = mid.replaceAll("\\\\", "/")
                    if( mid != "/" && _dir_exist(dir + mid) )
                        ant.move(todir: dir, includeEmptyDirs: "yes" ){
                            fileset(dir: dir+mid){
                                include(name: "**/*.*")
                                include(name: "**/*")
                            }
                        }
                }
            }
        }
    }
}


// process command line
if( args.length > 0 && args[0] == "-h" ){
            println '''usageg: NiuPlatform [-h|config]
    -h      show this message
    config  config file or path, default is '.'
            '''
    return;
}

properties.load(new File("./niuplatform.properties").newReader())

// create structure
ant.mkdir(dir: program_dir);
ant.mkdir(dir: data_dir);
ant.mkdir(dir: package_dir);

joomla.install();
//drupal.install();


//////////////////////////////////////////////////////////////
////  Base Operation
//////////////////////////////////////////////////////////////
abstract class Operation{

    public void require(){
        if( !check() ){
            install();
        }
    }

    String app = ""
    String adjust_search, adjust_destination;
    String program_name = "Joomla_3.0.3-Stable-Full_Package"
    String package_file = "${program_name}.zip"
    String program_dir = "${program_dir}/${program_name}"
    String data_dir = "${data_dir}/joomla-home"
    String download_url = "http://joomlacode.org/gf/download/frsrelease/17965/78414/${package_file}"

    public void download(){
        if( ! _file_exist("${package_dir}/${package_file}") )
            ant.get(src:download_url, dest:"${package_dir}/${package_file}", verbose:"on");
    }

    public boolean check(){
        return _dir_exist(program_dir)
    }

    protected abstract void install_required();

    public void install(){
        if( check() ) return;
        ant.echo(message:"install ${app}")
        ant.mkdir(dir: data_dir)

        install_required();

        if( !_file_exist("${package_dir}/${package_file}") )
            download();
        if( !_dir_exist(program_dir) )
            ant.unzip(src:"${package_dir}/${package_file}" , dest: program_dir);
        if( !check() ){
            _adjust_level(program_dir, adjust_search, adjust_destination)
            config()
        }
    }

    public abstract void config();

}


//////////////////////////////////////////////////////////////
////  apache
//////////////////////////////////////////////////////////////
class ApacheOp{

    public void require(){
        if( !apache.check() ){
            apache.install();
        }
    }

    String program_apache_name = "httpd-2.4.4-win32-ssl_0.9.8"
    String package_apache = "${program_apache_name}.zip"
    String program_apache_dir = "${program_dir}/${program_apache_name}"
    String data_apache_dir = "${data_dir}/www-home"
    String download_apache_url = "http://www.apachelounge.com/download/win32/binaries/${package_apache}"

    public void download(){
        if( ! _file_exist("${package_dir}/${package_apache}") )
            ant.get(src:download_apache_url, dest:"${package_dir}/${package_apache}", verbose:"on");
    }

    public boolean check(){
        return _dir_exist(program_apache_dir) &&
                _file_exist("${program_apache_dir}/bin/httpd.exe") &&
                _file_exist("${program_apache_dir}/conf/httpd.conf");
    }

    public void install(){
        if( apache.check() ) return;
        ant.echo(message:"install apache")
        ant.mkdir(dir: data_apache_dir)
        if( !_file_exist("${package_dir}/${package_apache}") )
            apache.download();
        if( !_dir_exist(program_apache_dir) )
            ant.unzip(src:"${package_dir}/${package_apache}" , dest: program_apache_dir);
        if( !apache.check() ){
            _adjust_level(program_apache_dir, "httpd.exe", "bin/httpd.exe")
            apache.config()
        }
    }

    public void config(){
        // append flag in httpd.conf
        ant.echo(message:"\r\n##NiuPlatform Begin##\r\n\r\n" +
                "##NiuPlatform Include##\r\n\r\n" +
                "##NiuPlatform End##\r\n",
                file:"${program_apache_dir}/conf/httpd.conf",
                append: "true", encoding:"utf-8")
        apache.config_set("ServerRoot", "\"${program_apache_dir}\"")
        apache.config_enable_module("rewrite_module", true)
        apache.config_change("Listen", "8080", "80")
        apache.config_set_multi("Listen", "8080")
        //apache.config_set_relat("DocumentRoot", data_apache_dir)
    }

    void config_change(String name, String value, String newValue){
        ant.replaceregexp(file:"${program_apache_dir}/conf/httpd.conf",
                byline:"true",
                match:"^(\\s*)${name}\\s+[\"\']?${value}[\"\']?\$",
                replace:"\\1${name} ${newValue}")
    }

    void config_set(String name, String value){
        ant.replaceregexp(file:"${program_apache_dir}/conf/httpd.conf",
                            byline:"true",
                            match:"^(\\s*)${name}(\\s+)[\"\']?(.+)[\"\']?\$",
                            replace:"\\1${name}\\2${value}")
    }

    void config_set_multi(String name, String multiValue){
        ant.replaceregexp(file:"${program_apache_dir}/conf/httpd.conf",
                byline:"true",
                match:"^((\\s*)${name}(\\s+)[\"\']?.+[\"\']?)\$",
                replace:"\\1\r\n\\2${name}\\3${multiValue}")
    }

    void config_enable_module(String name, boolean enable){
        apache.config_enable_line("LoadModule\\s+${name}", enable)
    }

    void config_enable_line(String line, boolean enable){
        String prefix = enable? "": "#"
        ant.replaceregexp(file:"${program_apache_dir}/conf/httpd.conf",
                byline:"true",
                match:"^(\\s*)#?(\\s*)(" + line + ")(\\s+.+)\$",
                replace:"${prefix}\\1\\2\\3\\4")
    }

    void config_include(String include){
        ant.replaceregexp(file:"${program_apache_dir}/conf/httpd.conf",
                byline:"true",
                match:"^\\s*(##NiuPlatform Include##)\\s*\$",
                replace:"\\1\r\nInclude ${include}")
    }
}

//////////////////////////////////////////////////////////////
////  php
//////////////////////////////////////////////////////////////
class PhpOp{

    public void require(){
        if( !php.check() ){
            php.install();
        }
    }

    String program_php_name = "php-5.4.13-Win32-VC9-x86"
    String package_php = "${program_php_name}.zip"
    String program_php_dir = "${program_dir}/${program_php_name}"
    String data_php_dir = "${data_dir}/php-home"
    String download_php_url = "http://windows.php.net/downloads/releases/${package_php}"

    public void download(){
        if( ! _file_exist("${package_dir}/${package_php}") )
            ant.get(src:download_php_url, dest:"${package_dir}/${package_php}", verbose:"on");
    }

    public boolean check(){
        return _dir_exist(program_php_dir) &&
                _file_exist("${program_php_dir}/php.exe") &&
                _file_exist("${program_php_dir}/php.ini");
    }

    public void install(){
        if( php.check() ) return;
        ant.echo(message:"install php")
        ant.mkdir(dir: "${data_php_dir}/session")
        ant.mkdir(dir: "${data_php_dir}/upload_tmp")
        apache.require()
        mysql.require()
        if( !_file_exist("${package_dir}/${package_php}") )
            php.download();
        if( !_dir_exist(program_php_dir) )
            ant.unzip(src:"${package_dir}/${package_php}" , dest: program_php_dir);
        if( !php.check() ){
            _adjust_level(program_php_dir, "php.exe", "php.exe")
            php.config()
        }
    }

    public void config(){
        ant.copy(file:"${program_php_dir}/php.ini-development",
                tofile:"${program_php_dir}/php.ini", overwrite:"true")

        String message = """#
# NiuPlatform php environment
#
PHPIniDir \"${program_php_dir}\"
LoadFile \"${program_php_dir}/php5ts.dll\"

# mysql lib
#LoadFile \"${mysql.program_mysql_dir}/lib/libmysql.dll\"

# microsoft sql server lib
# LoadFile \"${program_php_dir}/ntwdblib.dll\"

LoadModule php5_module \"${program_php_dir}/php5apache2_2.dll\"
AddType application/x-httpd-php .php
"""
        ant.echo(message: message, 
                file:"${apache.program_apache_dir}/conf/extra/niu-php.conf")
        apache.config_include("conf/extra/niu-php.conf")

        ant.replaceregexp(file:"${apache.program_apache_dir}/conf/extra/niu-php.conf",
                byline:"true",
                match:"^(\\s*LoadFile\\s+).+/lib/libmysql\\.dll(.+)\$",
                replace:"\\1 \"${program_mysql_dir}/lib/libmysql.dll\"")

        php.config_set("max_execution_time", "30")
        php.config_set("session.save_path", "\"${data_php_dir}/session\"")
        php.config_set("upload_tmp_dir", "\"${data_php_dir}/upload_tmp\"")
        php.config_set("date.timezone", "Asia/Shanghai")

        php.config_set("extension_dir", "\"${program_php_dir}/ext\"")
        php.config_enable("extension_dir", true)
        php.config_enable_extension("php_curl", true)
        php.config_enable_extension("php_gd2", true)
        php.config_enable_extension("php_mbstring", true)
        php.config_enable_extension("php_xmlrpc", true)
        php.config_enable_extension("php_xsl", true)
    }


    void config_set(String name, String value){
        ant.replaceregexp(file:"${program_php_dir}/php.ini",
                byline:"true",
                match:"^(\\s*)${name}(\\s*=\\s*)[\"\']?(.+)[\"\']?\\s*\$",
                replace:"\\1${name}\\2${value}")
    }

    void config_enable(String name, boolean enable){
        php.config_enable_line("${name}\\s*=\\s*", enable)
    }

    void config_enable_extension(String extension, boolean enable){
        php.config_enable_line("extension\\s*=\\s*${extension}\\.dll", enable)
    }

    void config_enable_line(String line, boolean enable){
        String prefix = enable? "": ";"
        ant.replaceregexp(file:"${program_php_dir}/php.ini",
                byline:"true",
                match:"^(\\s*);?(\\s*)(" + line + ")(\\s+.+)\$",
                replace:"${prefix}\\1\\2\\3\\4")
    }

}

//////////////////////////////////////////////////////////////
////  mysql
//////////////////////////////////////////////////////////////
class MysqlOp{

    public void require(){
        if( !mysql.check() ){
            mysql.install();
        }
    }

    String program_mysql_name = "mysql-5.6.10-win32"
    String package_mysql = "${program_mysql_name}.zip"
    String program_mysql_dir = "${program_dir}/${program_mysql_name}"
    String data_mysql_dir = "${data_dir}/mysql32-home"
    String download_mysql_url = //"http://www.mysql.com/get/Downloads/MySQL-5.6/${package_mysql}/from/http://cdn.mysql.com/"
                                  "http://cdn.mysql.com/Downloads/MySQL-5.6/${package_mysql}"
    public void download(){
        if( ! _file_exist("${package_dir}/${package_mysql}") )
            ant.get(src:download_mysql_url, dest:"${package_dir}/${package_mysql}", verbose:"on");
    }

    public boolean check(){
        return _dir_exist(program_mysql_dir) &&
                _file_exist("${program_mysql_dir}/bin/mysql.exe") &&
                _file_exist("${program_mysql_dir}/my.ini");
    }

    public void install(){
        if( mysql.check() ) return;
        ant.echo(message:"install mysql")
        ant.mkdir(dir: data_mysql_dir)

        if( !_file_exist("${package_dir}/${package_mysql}") )
            mysql.download();
        if( !_dir_exist(program_mysql_dir) )
            ant.unzip(src:"${package_dir}/${package_mysql}" , dest: program_mysql_dir);
        if( !mysql.check() ){
            _adjust_level(program_mysql_dir, "mysql.exe", "bin/mysql.exe")
            mysql.config()
        }
    }

    public void config(){
        ant.copy(file: _file_exist("${program_mysql_dir}/my-small.ini")? "${program_mysql_dir}/my-small.ini": "${program_mysql_dir}/my-default.ini",
                tofile:"${program_mysql_dir}/my.ini", overwrite:"true")
        ant.copy(todir:data_mysql_dir, overwrite:"true", includeEmptyDirs:"true"){
            fileset(dir:"${program_mysql_dir}/data")
        }

        mysql.config_set_add("mysqld", "character-set-server", "utf8")
        mysql.config_set_add("mysqld", "basedir", program_mysql_dir)
        mysql.config_set_add("mysqld", "datadir", data_mysql_dir)
    }

    void config_set_add(String section, String name, String value){
        ant.replaceregexp(file:"${program_mysql_dir}/my.ini",
                byline:"true",
                match:"^\\s*\\[${section}\\]\\s*\$",
                replace:"\\[${section}\\]\r\n${name}=${value}")
    }

    void config_set(String section, String name, String value){
        ant.replaceregexp(file:"${program_mysql_dir}/my.ini",
                byline:"false",
                match:"^(\\s*)${name}(\\s*=\\s*)[\"\']?(.+)[\"\']?\\s*\$",
                replace:"\\1${name}\\2${value}")
    }

    void config_enable(String name, boolean enable){
        mysql.config_enable_line("${name}\\s*=\\s*", enable)
    }

    void config_enable_extension(String extension, boolean enable){
        mysql.config_enable_line("extension\\s*=\\s*${extension}\\.dll", enable)
    }

    void config_enable_line(String line, boolean enable){
        String prefix = enable? "": ";"
        ant.replaceregexp(file:"${program_mysql_dir}/mysql.ini",
                byline:"true",
                match:"^(\\s*);?(\\s*)(" + line + ")(\\s+.+)\$",
                replace:"${prefix}\\1\\2\\3\\4")
    }

}

//////////////////////////////////////////////////////////////
////  Joomla
//////////////////////////////////////////////////////////////
class JoomlaOp{

    public void require(){
        if( !joomla.check() ){
            joomla.install();
        }
    }

    String program_joomla_name = "Joomla_3.0.3-Stable-Full_Package"
    String package_joomla = "${program_joomla_name}.zip"
    String program_joomla_dir = "${program_dir}/${program_joomla_name}"
    String data_joomla_dir = "${data_dir}/joomla-home"
    String download_joomla_url = "http://joomlacode.org/gf/download/frsrelease/17965/78414/${package_joomla}"

    public void download(){
        if( ! _file_exist("${package_dir}/${package_joomla}") )
            ant.get(src:download_joomla_url, dest:"${package_dir}/${package_joomla}", verbose:"on");
    }

    public boolean check(){
        return _dir_exist(program_joomla_dir) &&
                _file_exist("${program_joomla_dir}/index.php") &&
                _file_exist("${program_joomla_dir}/joomla.xml");
    }

    public void install(){
        if( joomla.check() ) return;
        ant.echo(message:"install joomla")
        ant.mkdir(dir: data_joomla_dir)

        apache.require();
        php.require();
        mysql.require();

        if( !_file_exist("${package_dir}/${package_joomla}") )
            joomla.download();
        if( !_dir_exist(program_joomla_dir) )
            ant.unzip(src:"${package_dir}/${package_joomla}" , dest: program_joomla_dir);
        if( !joomla.check() ){
            _adjust_level(program_joomla_dir, "joomla.exe", "bin/joomla.exe")
            joomla.config()
        }
    }

    public void config(){
        php.config_enable_extension("php_curl", true)
        php.config_enable_extension("php_dba", true)
        php.config_enable_extension("php_exif", true)
        php.config_enable_extension("php_mysql", true)
        php.config_enable_extension("php_mysqli", true)
        php.config_enable_extension("php_pdo_mysql", true)
        php.config_enable_extension("php_pdo_odbc", true)
        php.config_enable_extension("php_gd2", true)
        php.config_enable_extension("php_intl", true)
        php.config_enable_extension("php_ldap", true)
        php.config_enable_extension("php_mbstring", true)
        php.config_enable_extension("php_ming", true)
        php.config_enable_extension("php_xmlrpc", true)
        php.config_enable_extension("php_xsl", true)
        php.config_enable_extension("php_dba", true)
    }

}




