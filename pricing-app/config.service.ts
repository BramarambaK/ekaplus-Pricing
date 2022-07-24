import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, throwError, BehaviorSubject } from 'rxjs';
import { EnvConfig, Environment } from '@eka-framework/core';
import { retry, catchError, concatMap } from 'rxjs/operators';
import { ApplicationService } from '@app/views/application/application.service';
import { RequestOptions } from '@angular/http';

@Injectable({
  providedIn: 'root'
})
export class PricingService {
  formvalues: any;
  values: any;
  formulaListToForm;
  fromPricing: any;
  globaldateformat = 'DD-MM-YYYY'
  uuid: any;
  httpOptions = {
    headers: new HttpHeaders({
      'Content-Type': 'application/json',
      Authorization: 'my-auth-token'
    })
  };
  //for contract pop
  private selectedContractFormula = new BehaviorSubject('');
  contractFormula = this.selectedContractFormula.asObservable();

  private toggleComponent = new BehaviorSubject('listing');
  currentComponent = this.toggleComponent.asObservable();

  outerModal;

  configUrlData;

  configUrlCurveData;
  configUrlMeta;
  curveMeta;
  configUrlPost;
  configUrlCurveDataPost;
  delete;
  contractRefURL;
  mdmMeta;
  baseCurve;
  pricing_URL;
  seedingCurves;
  associatedFormulaApi;
  app;
  contractObj: boolean =  false;
  constructor(
    private http: HttpClient,
    private appService: ApplicationService
  ) {
    this.appService.appMeta$.subscribe((app: any) => {
      if (app.name !== 'pricing') {
        this.http.post('/meta/app/pricing',{}).subscribe((res: any) => {
          this.uuid = res.sys__UUID;

          this.configUrlData = `/data/${this.uuid}/formula`;

          this.configUrlCurveData = `/data/${this.uuid}/curve`;

          this.configUrlPost = `/data/${this.uuid}/formula`;
          this.configUrlCurveDataPost = `/data/${this.uuid}/curve`;
          this.delete = `/data/${this.uuid}/formula`;
          this.contractRefURL = `/data/${
            this.uuid
            }/contract?internalContractRefNo=`;
          this.baseCurve = `/data/${this.uuid}/baseCurve`;

          this.configUrlMeta = '/meta/object/formula';
          this.curveMeta = '/meta/object/curve';
          this.mdmMeta = EnvConfig.vars.eka_mdm_host + `/mdm/${this.uuid}/data`;

          this.pricing_URL =
            EnvConfig.vars.eka_pricing_host +
            '/api/pricing/formula?mode=Detailed';
          this.seedingCurves =
            EnvConfig.vars.eka_pricing_host +
            '/curve/seedCurveData/pricing/baseCurve';
          this.associatedFormulaApi =
            EnvConfig.vars.eka_pricing_host + '/api/pricing/formula/edit';
        });
      } else if (app.name === 'pricing') {
        this.uuid = app.sys__UUID;

        this.configUrlData = `/data/${this.uuid}/formula`;

        this.configUrlCurveData = `/data/${this.uuid}/curve`;

        this.configUrlPost = `/data/${this.uuid}/formula`;
        this.configUrlCurveDataPost = `/data/${this.uuid}/curve`;
        this.delete = `/data/${this.uuid}/formula`;
        this.contractRefURL = `/data/${
          this.uuid
          }/contract?internalContractRefNo=`;
        this.baseCurve = `/data/${this.uuid}/baseCurve`;

        this.configUrlMeta = '/meta/object/formula';
        this.curveMeta = '/meta/object/curve';
        this.mdmMeta = EnvConfig.vars.eka_mdm_host + `/mdm/${this.uuid}/data`;

        this.pricing_URL =
          EnvConfig.vars.eka_pricing_host +
          '/api/pricing/formula?mode=Detailed';
        this.seedingCurves =
          EnvConfig.vars.eka_pricing_host +
          '/curve/seedCurveData/pricing/baseCurve';
        this.associatedFormulaApi =
          EnvConfig.vars.eka_pricing_host + '/api/pricing/formula/edit';
      }
    });
  }

  changeCurrentComponent(currentComponent: string) {
    this.toggleComponent.next(currentComponent);
  }

  changeContractFormula(formulaId: any) {
    this.selectedContractFormula.next(formulaId);
  }

  getConfig() {
    const options = {
      headers: {
        'X-ObjectAction': 'READ'
      }
    };
    return this.http.get(this.configUrlData, options);
  }
  getConfigById(id) {
    return this.http.get(this.configUrlData + '/' + id);
  }
  getCurveData() {
    const options = {
      headers: {
        'X-ObjectAction': 'READ'
      }
    };
    return this.http.get(this.configUrlCurveData, options);
  }
  getConfigMeta() {
    const options = {
      headers: {
        'X-ObjectAction': 'READ'
      }
    };

    return this.http.get(this.configUrlMeta, options);
  }

  getCurveMeta() {
    const options = {
      headers: {
        'X-ObjectAction': 'READ'
      }
    };
    return this.http.get(this.curveMeta, options);
  }
  postConfigData(formulaData: any): Observable<any> {
    const options = {
      responseType: 'text' as 'text',
      headers: {
        'X-ObjectAction': 'CREATE'
      }
    };
    return this.http.post(this.configUrlPost, formulaData, options);
  }
  putConfigData(formulaData: any, id): Observable<any> {
    const options = {
      responseType: 'text' as 'text',
      headers: {
        'X-ObjectAction': 'UPDATE'
      }
    };
    return this.http.put(this.configUrlPost + '/' + id, formulaData, options);
  }
  postConfigCurveDataPost(curveData: any): Observable<any> {
    const options = {
      responseType: 'text' as 'text',
      headers: {
        'X-ObjectAction': 'CREATE'
      }
    };
    return this.http.post(this.configUrlCurveDataPost, curveData, options);
  }
  associatedFormula(formula_id) {
    const options = {
      headers: {
        'Content-Type': 'application/json'
      }
    };
    return this.http.get(this.associatedFormulaApi + '/' + formula_id, options);
  }
  deleteFormula(id: number): Observable<{}> {
    const options = {
      responseType: 'text' as 'text',
      headers: {
        'X-ObjectAction': 'DELETE'
      }
    };
    const url = `${this.delete}/${id}`; // DELETE api/heroes/42
    return this.http.delete(url, options);
  }

  // getMdmMeta(mdmdata: any): Observable<any> {
  //   return this.http.post<any>(this.mdmMeta, mdmdata);
  // }
  getMdmMeta(workFlowTask,mdmdata: any){

    var url = '/workflow/mdm';
        let workFlowConfigBody = {
          appId: this.uuid,
          data: mdmdata,
          workFlowTask: workFlowTask,
          payLoadData: ''
        };
        return this.http.post(url, workFlowConfigBody);
  }

  setValue(val) {
    this.formvalues = val;
  }

  getValue() {
    return this.formvalues;
  }

  formvaluepost(workflowtask,values) {
    this.postApiWorkflow(workflowtask,values).subscribe((data: any) => {
      console.log('formservice');
      this.changeContractFormula(data.data._id);
      return data.data;
    });
  }
  formvalueput(workflowtask,values, id) {
    this.putApiWorkflow(workflowtask,values, id).subscribe((data: any) => {
      console.log('formservice');
      this.changeContractFormula(data.data._id);
      return data.data;
    });
  }

  GetHeadingPost(finalPrice: any): Observable<any> {
    return this.http.post<any>(this.pricing_URL, finalPrice);
  }

  getBaseCurve() {
    return this.http.get(this.baseCurve);
  }
  seedCurves() {
    return this.http.post(this.seedingCurves, {});
  }
  // handleError(error) {
  //   let errorMessage = '';
  //   if (error.error instanceof ErrorEvent) {
  //     // client-side error
  //     errorMessage = `Error: ${error.error.message}`;
  //   } else {
  //     // server-side error
  //     errorMessage = `Error Code: ${error.status}\nMessage: ${error.message}`;
  //   }
  //   //window.alert(errorMessage);
  //   return errorMessage;
  // }

  // getContractDataFromRefNo(contractRefNo) {
  //   return this.http.get('/meta/app/5d907cd2-7785-4d34-bcda-aa84b2158415').pipe(
  //     concatMap((app: any) => {
  //       return this.http.get(
  //         `/data/${app.sys__UUID}/contract?internalContractRefNo=` +
  //         contractRefNo
  //       );
  //     })
  //   );
  // }
  getContractDataFromRefNo(contractRefNo) {
    this.contractObj = true
    return this.getApiWorkflowForContract('contract_list',contractRefNo)
}


getApiWorkflowForContract(workflowtask,id) {
  workflowtask = {
    appId: this.uuid,
    workFlowTask: workflowtask
  }
  let url = `/workflow/data?internalContractRefNo=` + id;
  workflowtask.appId = '5d907cd2-7785-4d34-bcda-aa84b2158415'
 
  return this.http
    .post(url, workflowtask, this.httpOptions)
}

  getApiWorkflow(workflowtask, id = false,payLoadData:any=false) {
    workflowtask = {
      appId: this.uuid,
      workFlowTask: workflowtask
    }
    let url = `/workflow/data`;
    if (id !== false) {
      url = url + '?_id=' + id
    }
    if (payLoadData !== false) {
      workflowtask['payLoadData']=payLoadData
    }
    if(this.contractObj){
      workflowtask.appId = '5d907cd2-7785-4d34-bcda-aa84b2158415'
    }
    this.contractObj=false
    return this.http
      .post(url, workflowtask, this.httpOptions)
  }

  deleteApiWorkflow(workflowtask, id,sys__UUID ) {
    let url = `/workflow`;
    workflowtask = {
      appId: this.uuid,
      workflowTaskName: workflowtask,
      task: workflowtask,
      output: {
        [workflowtask]: {
          sys__UUID : sys__UUID
        }
      },
      id: id
    }
    return this.http
    .post(url, workflowtask, this.httpOptions)
  }

  postApiWorkflow(workflowtask,data){
    let url = `/workflow`;
    workflowtask = {
      appId: this.uuid,
      workflowTaskName: workflowtask,
      task: workflowtask,
      output: {
        [workflowtask]: data
      }
    }
    return this.http
    .post(url, workflowtask, this.httpOptions)

  }

  putApiWorkflow(workflowtask,data,id){
    let url = `/workflow`;
    workflowtask = {
      appId: this.uuid,
      workflowTaskName: workflowtask,
      task: workflowtask,
      output: {
        [workflowtask]: data
      },
      id: id
    }
    return this.http
    .post(url, workflowtask, this.httpOptions)
  }
  getComponent(contractDetails) {
    var workflowtask = {
      appId: '5d907cd2-7785-4d34-bcda-aa84b2158415',
      workFlowTask: 'component_list'
    }
    let url = `/workflow/data`;
    url = url + '?componentDraftId=' +contractDetails._id
    return this.http.post(url, workflowtask, this.httpOptions)
  }
  layout(workflow){
    let url = `/workflow/layout`;
    let workflowtask = {
      appId: this.uuid,
      workFlowTask: workflow
    }
    return this.http
    .post(url, workflowtask, this.httpOptions)
  }
}
