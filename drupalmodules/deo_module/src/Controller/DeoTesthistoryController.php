<?php
/**
 * Created by PhpStorm.
 * User: dsn1
 * Date: 12/12/16
 * Time: 2:12 PM
 */


namespace Drupal\deo_module\Controller;

use Symfony\Component\HttpFoundation\Response;
use Drupal\deo_module\DeoData;
use Drupal\block_example\Plugin\Block;
use \Drupal\node\Entity\Node;
use \Drupal;

class DeoTesthistoryController {

    public function content($count) {

        // Call service
        $dataCollector = new DeoData\DataCollector();
        $output = \GuzzleHttp\json_decode($dataCollector->getModified($count),true);

        $testtemp = array (
            '#theme' => 'deo_history',
            '#record_title' => 'Record History',
            '#record_modified' =>  $output[0]["modified"],
        );

        return $testtemp;

    }

}
