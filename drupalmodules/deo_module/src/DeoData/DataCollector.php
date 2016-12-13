<?php

/**
 * Created by PhpStorm.
 * User: dsn1
 * Date: 10/25/16
 * Time: 1:22 PM
 */
namespace Drupal\deo_module\DeoData;

class DataCollector
{
    public function getData($arg){
        $client = \Drupal::service('http_client');
        $result = $client->get('http://10.200.222.250:8082/RMMApi/records/search/'.$arg, ['Accept' => 'application/json']);
        $output = $result->getBody();
        return $output;
    }

    public function getModified($arg){

        $client = \Drupal::service('http_client');
        $result = $client->get('http://10.200.222.250:8082/RMMApi/records/search/'.$arg, ['Accept' => 'application/json']);
        $output = $result->getBody();
        return $output;
    }
    public function getList($arg){

        $client = \Drupal::service('http_client');
        $result = $client->get('http://10.200.222.250:8082/RMMApi/records/advancedsearch?theme='.$arg, ['Accept' => 'application/json']);
        $output = $result->getBody();
        return $output;
    }
}