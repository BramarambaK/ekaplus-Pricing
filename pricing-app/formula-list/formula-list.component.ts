import {
  Component,
  OnInit,
  NgZone,
  HostListener,
  Input,
  ViewChild
} from '@angular/core';

import { ActivatedRoute, Router } from '@angular/router';
import { PricingService } from '../config.service';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { TableService } from '../table.service';
import { headersToString, del } from 'selenium-webdriver/http';
import { filter } from 'rxjs/operators';
import { ApplicationService } from '@app/views/application/application.service';

@Component({
  selector: 'app-formula-list',
  templateUrl: './formula-list.component.html',
  styleUrls: ['./formula-list.component.scss']
})
export class FormulaListComponent implements OnInit {
  responseData: any;
  responseMeta = {
    fields: ''
  };
  records: any;
  selectedRow;
  setClickedRow: Function;
  @Input() fromPricing = true;
  @Input() valuation = false;
  outerModal;
  formulaDetails;
  currentPage = 1;
  itemsPerPage = 8;
  pageSize: number;
  searchList: any;
  searchText;
  tableheadings;
  header: any;
  filteredMeta;
  showActions = false;
  selected;
  allowed;
  associated = false;
  showMassage = 'Formula Deleted!!';
  @ViewChild('confirm') deletePopup;

  constructor(
    private ngZone: NgZone,
    private configService: PricingService,
    private modalService: NgbModal,
    private router: Router,
    private route: ActivatedRoute,
    private tableService: TableService,
    private appService: ApplicationService
  ) {
    this.outerModal = this.configService.outerModal;

    window.onresize = e => {
      //ngZone.run will help to run change detection
      this.ngZone.run(() => {
        if (window.innerWidth <= 768) {
          this.showActions = true;
        }
        if (window.innerWidth > 768) {
          this.showActions = false;
        }
      });
    };

    //get formula list data
    this.configService.getApiWorkflow('formula_list').subscribe((responseData: any) => {
      this.records = responseData.data;

      this.records = this.records.filter(this.isRealOne);
      console.log(this.records);

      if (this.valuation === true) {
        this.records = this.records.filter(res => {
          if (res.category === 'M2M') {
            return true;
          }
        });
      }
    });
    //get formula list meta
    this.configService.getConfigMeta().subscribe((res: any) => {
      if (this.fromPricing) {
        this.allowed = ['formulaName', 'newFormulaExp', 'category'];
      } else {
        this.allowed = ['formulaName', 'newFormulaExp'];
      }
      this.header = this.tableService.formatHeader(res.fields, this.allowed);
      console.log(this.header);
    });
  }
  isRealOne(res) { 
    if (res['_autoCreated'] !== true && res['isComponentAssociated']!=true) return true;
    else return false;
  }

  submitFilter() {
    this.searchList = this.searchText;
  }
  ngOnInit() {
    console.log(this.responseMeta);
    let curveList = Object.values(this.responseMeta.fields);
    if (this.fromPricing) this.appService.setTitle('Formula List');
    console.log(curveList);
  }

  //pagination
  public onPageChange(pageNum: number): void {
    this.pageSize = this.itemsPerPage * (pageNum - 1);
  }

  public changePagesize(num: number): void {
    this.itemsPerPage = this.pageSize + num;
  }
  selectedFormula(formulaDetails) {
    this.configService.formulaListToForm = formulaDetails;
    this.toggle();
  }
  clearBeforeToggle() {
    this.configService.formulaListToForm = null;
    this.toggle();
  }

  toggle() {
    this.configService.changeCurrentComponent('formulaForm');
  }

  //to delete formula
  delete(index, deletemodal) {
    // this.configService.deleteFormula(index._id).subscribe(data => {});
    this.configService.getApiWorkflow('formula_list',index._id).subscribe(
      (data: any) => {
        this.associated = data;
        if (this.associated) {
          this.selectedRow = index;
          this.configService.deleteApiWorkflow('delete_formula',index._id,index.sys__UUID).subscribe(data => {
            this.deletePopup.open('Formula is deleted');
            this.records.splice(this.selectedRow, 1);
          });
        } else {
          this.deletePopup.open('This is Associated With Some Contract');
        }
      },
      err => {
        this.deletePopup.open('Api error');
      }
    );
  }

  setFormulaDetail(formula, index) {
    this.configService.changeContractFormula(formula._id);
    this.selectedRow = index;
  }

  edit(formulaDetails, deletemodal) {
    this.configService.getApiWorkflow('formula_list',formulaDetails._id).subscribe(
      (data: any) => {
        if (data === true) {
          this.router.navigate(['../edit'], {
            queryParams: { id: formulaDetails._id },
            relativeTo: this.route
          });
        } else {
          this.deletePopup.open('This is Associated With Some Contract');
        }
      },
      err => {
        this.deletePopup.open('Api error');
      }
    );
  }

  copy(formulaDetails) {
    this.router.navigate(['../copy'], {
      queryParams: { id: formulaDetails._id },
      relativeTo: this.route
    });
  }

  onChange(selected) {
    console.log(selected);
  }

  // Filters the data for distinct values for a column to be used in Table filter
  filter_distinct(array, property) {
    var unique = {};
    var distinct = [];
    for (var i in array) {
      if (typeof unique[array[i][property]] == 'undefined') {
        distinct.push({ label: array[i][property], value: array[i][property] });
      }
      unique[array[i][property]] = 0;
    }
    return distinct;
  }

  //If display record is a object it display's its value otherwise prints the string
  display(obj) {
    if (obj.value) return obj.value;
    else return obj;
  }

  displayHeader(obj: any) {
    if (obj.labelKey) return obj[obj.labelKey];
    else return obj;
  }

  isSort(obj) {
    if (typeof obj.sort !== 'undefined') return obj.sort;
    else return true;
  }

  isFilter(obj) {
    if (typeof obj.filter !== 'undefined') return obj.filter;
    else return true;
  }

  useFormulaInContract() {
    delete this.selected.sys__data__state
    delete this.selected.userId
    delete this.selected.sys__createdBy
    delete this.selected.sys__createdOn
    delete this.selected._id
    this.selected['_autoCreated'] = true
    this.selected.triggerPricing = []
    this.selected.priceDifferential = [ 
      {
          "differentialType" : "Discount",
          "diffLowerThreashold" : "",
          "differentialValue" : "",
          "diffUpperThreshold" : "",
          "differentialUnit" : ""
      }, 
      {
          "differentialType" : "S-Curve",
          "diffLowerThreashold" : "",
          "differentialValue" : "",
          "diffUpperThreshold" : "",
          "differentialUnit" : ""
      }
  ]
    this.configService.postApiWorkflow('formula_save',this.selected).subscribe((data: any) => {
      this.configService.changeContractFormula(data.data._id);
      this.outerModal.dismiss('formula select from list');
    });
  }
}
