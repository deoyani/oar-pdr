import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http'; 
import * as data_json   from '../../../assets/environment.json';
import {  PLATFORM_ID, APP_ID, Inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

export interface Config {
    RMMAPI: string;
    DISTAPI: string;
    LANDING: string
    SDPAPI: string;
    PDRAPI: string;
    METAPI: string; 
 }
@Injectable()
export class AppConfig {
    private confValues : Config ;
    private confCall : any;
    private envVariables = "./assets/environment.json";
    private appConfig;

    constructor(private http: HttpClient
        ,@Inject(PLATFORM_ID) private platformId: Object,
        @Inject(APP_ID) private appId: string) { }

    loadAppConfig() {
        if(isPlatformBrowser(this.platformId)){
        console.log("Test here:"+this.envVariables);

      this.confCall =  this.http.get<Config>(this.envVariables) 
                        .toPromise()
                        .then(
                            resp =>{
                                resp as Config
                                console.log("TEST 0 in promise then"+ resp);
                                this.setConfig(resp);
                            },
                            err => {
                                console.log("ERROR IN CONFIG :"+err);
                            }
                        );
       return this.confCall; 
     }
     else{
         
         
        this.appConfig = <any>data_json;
        console.log("process.env.RMMAPI :"+process.env.RMMAPI);
         console.log("this.appConfig :"+this.appConfig);
         console.log("this.appConfig.RMMAPI"+this.appConfig.RMMAPI);
       this.confValues.RMMAPI = process.env.RMMAPI || this.appConfig.RMMAPI;
       this.confValues.DISTAPI =  process.env.DISTAPI || this.appConfig.DISTAPI;
       this.confValues.LANDING = process.env.LANDING || this.appConfig.LANDING;
       this.confValues.METAPI = process.env.METAPI || this.appConfig.METAPI;
       this.confValues.SDPAPI  = process.env.SDPAPI || this.appConfig.SDPAPI;
       this.confValues.PDRAPI =  process.env.PDRAPI || this.appConfig.PDRAPI;

     }
        }

    getConfig() {
        return this.confValues;
    }
    setConfig(confValues: Config){
        this.confValues = confValues;
    }
}