<?php
/**
 * Created by PhpStorm.
 * User: dsn1
 * Date: 10/24/16
 * Time: 2:22 PM
 */

namespace Drupal\deo_module\Controller;

use Symfony\Component\HttpFoundation\Response;
use Drupal\deo_module\DeoData;
use Drupal\block_example\Plugin\Block;
use \Drupal\node\Entity\Node;
use \Drupal;

class DeoTestController {
    public function mymodule_node_title_exists($title) {
        return db_query("SELECT nid FROM {node_field_data} WHERE title = :title", array(':title' => $title))->fetchField();
        //return "Test";
    }
    public function content($count) {

       // Call service
      $dataCollector = new DeoData\DataCollector();
      $output = \GuzzleHttp\json_decode($dataCollector->getData($count),true);

        $summarypage = 'http://10.200.94.245:8080/#/search?identifier='.$output[0]["identifier"];


        $form['file_example_fid'] = array(
            '#title' => t('Image'),
            '#type' => 'managed_file',
            '#description' => t('Upload your picture.'),
            '#upload_location' => 'public://example_files/',
        );

        $testtemp = array (
          '#theme' => 'deo_module',
          '#record_title'=> $output[0]["title"],
          '#record_contact'=> $output[0]["contactPoint"]["fn"],
          '#record_contactmail'=> $output[0]["contactPoint"]["hasEmail"],
          '#addressline1' => 'NIST Headquarters',
          '#addressline2' => '100 Bureau Dr',
          '#addressline3' => 'Gaithersburg, MD 20899',
          '#record_doi'=> $output[0]["distribution"][0]["accessURL"],
          '#record_publishername' =>  $output[0]["publisher"]["name"],
          '#record_description' => $output[0]["description"],
          '#record_keywords' => $output[0]["keyword"],
          '#record_landingpage' => $output[0]["landingpage"],
          '#record_link' => $output[0]["distribution"],
          '#record_summary' => $summarypage,
          '#record_id' => $count,
      );


//        $node = Node::create([
//            'type'        => 'page',
//            'title'       => $output[0]["title"],
//            'path'        => '/nodecreated/1',
//            'body'        => $testtemp,
//
//        ]);
//
//        if (!$this->mymodule_node_title_exists($output[0]["title"])) {
//            //node_save($node);
//            $node->save();
//            $temp = 'new';
//        }

//        $data = array(
//            'type' => 'testcontent',
//            'title' => 'TestTemplate 7',
//            //'field_body' => '***TEST This Again !!!!',
//            '#record_title'=> $output[0]["title"],
//            '#record_contact'=> $output[0]["contactPoint"]["fn"],
//            '#record_contactmail'=> $output[0]["contactPoint"]["hasEmail"],
//            '#addressline1' => 'NIST Headquarters',
//            '#addressline2' => '100 Bureau Dr',
//            '#addressline3' => 'Gaithersburg, MD 20899',
//        );
//        $node = Drupal::entityManager()
//            ->getStorage('node')
//            ->create($data);
//        $node->save();

        return $testtemp;

    }

}
