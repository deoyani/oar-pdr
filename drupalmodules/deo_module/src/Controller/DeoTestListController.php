<?php
/**
 * Created by PhpStorm.
 * User: dsn1
 * Date: 12/13/16
 * Time: 10:38 AM
 */
namespace Drupal\deo_module\Controller;

use Symfony\Component\HttpFoundation\Response;
use Drupal\deo_module\DeoData;
use Drupal\block_example\Plugin\Block;
use \Drupal\node\Entity\Node;
use \Drupal;

class DeoTestListController {

    public function content() {

        // Call service
        $dataCollector = new DeoData\DataCollector();
        $chem = \GuzzleHttp\json_decode($dataCollector->getList("Chemistry"),true);
        $phy = \GuzzleHttp\json_decode($dataCollector->getList("Physics"),true);
        $bio = \GuzzleHttp\json_decode($dataCollector->getList("Biometrics"),true);
        $hum = \GuzzleHttp\json_decode($dataCollector->getList("Human"),true);

        $testtemp = array (
            '#theme' => 'deo_listrecord',
            '#record_chem' => $chem,
            '#record_phy' =>  $phy,
            '#record_bio' => $bio,
            '#record_hum' => $hum,
        );

        return $testtemp;

    }

}
