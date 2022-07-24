import {
  Component,
  AfterViewInit,
  OnInit,
  Output,
  EventEmitter,
  ViewChild,
  ElementRef,
  Input,
  ViewEncapsulation
} from '@angular/core';

import {
  NgbModal,
  ModalDismissReasons,
  NgbActiveModal
} from '@ng-bootstrap/ng-bootstrap';
import { PricingService } from '../../../config.service';
import { objectExpression } from 'babel-types';
import { TableService } from '../../../table.service';

@Component({
  selector: 'app-modal',
  templateUrl: './modal.component.html',
  styleUrls: ['./modal.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ModalComponent implements OnInit {
  curveData;
  newModalContent;
  setClickedRow: Function;
  selectedRow;
  selectedExpression;
  modalReference;
  currentPage = 1;
  itemsPerPage = 6;
  pageSize: number;
  curveMeta;
  curveheadings;
  filteredMeta;
  records;
  header;
  selected;
  first = 1;
  pageIndex = 1;
  // content = "content";
  @Output() valueChange = new EventEmitter();
  @ViewChild('content') content: ElementRef;
  @ViewChild('dt') dataTable;

  @Input() curveBaseDataModal;
  firstIndex: number = 1;
  lastindex: number = 10;
  constructor(
    private modalService: NgbModal,
    private tableService: TableService,
    private configService: PricingService
  ) {
    this.setClickedRow = function (index) {
      this.selectedRow = index;
    };
  }

  ngOnInit() { }
  ngAfterViewInit() { }
  toggle() {
    this.modalReference.close();
  }

  open(content) {
    this.firstIndex = 1;
    this.lastindex = 10;
    this.modalReference = this.modalService.open(content, {
      size: 'lg'
    });
    this.configService.getCurveMeta().subscribe((response: any) => {
      const allowed = ['curveName', 'curveExpression'];
      this.header = this.tableService.formatHeader(response.fields, allowed);

      // const allowed = ['curveName', 'curveExpression'];

      // this.filteredMeta = Object.keys(this.curveMeta)
      //   .filter(key => allowed.includes(key))
      //   .reduce((obj, key) => {
      //     obj[key] = this.curveMeta[key];
      //     return obj;
      //   }, {});

      // console.log(this.filteredMeta);
      //     let objnew: object;

      //     objnew = this.curveMeta.curveName
      //     let car: any;
      //     console.log(this.curveMeta);
      //     for (let i = 0; i < Object.keys(this.curveMeta).length; i++) {
      //       if (Object.keys(this.curveMeta)[i] === "curveName" || Object.keys(this.curveMeta)[i] === "curveExpression")
      //         car = this.curveMeta;
      //     }

      // console.log(response);
      // this.curveheadings = Object.keys(this.curveMeta.fields).map(
      //   key => this.curveMeta.fields[key]
      // );
      // console.log(this.curveheadings);
    });
    // this.configService.getCurveData().subscribe(response => {
    //   this.records = response;
    //   console.log(response);
    //   console.log(this.curveData);
    // });
    this.records = this.curveBaseDataModal;
  }

  //SELECTING rows
  onSelect() {
    this.selectedExpression = this.selected;
    console.log(this.selectedExpression);
    this.valueChange.emit(this.selectedExpression);

    // this.modalServiceNew.close(this.newModalContent);
    this.modalReference.close();
  }
  //pagination
  public onPageChange(pageNum: number): void {
    this.pageSize = this.itemsPerPage * (pageNum - 1);
  }

  public changePagesize(num: number): void {
    this.itemsPerPage = this.pageSize + num;
  }

  setCurrentPage(n: number) {
    let paging = {
      first: (n - 1) * this.dataTable.rows,
      rows: this.dataTable.rows
    };
    this.dataTable.paginate(paging);
  }
  paginate(event) {
    //event.first: Index of first record being displayed
    //event.rows: Number of rows to display in new page
    //event.page: Index of the new page
    //event.pageCount: Total number of pages
    this.pageIndex = event.first / event.rows + 1; // Index of the new page if event.page not defined.
    this.firstIndex = (this.pageIndex - 1) * event.rows + 1;
    this.lastindex = this.pageIndex * event.rows;
    if (this.records.length < this.lastindex)
      this.lastindex = this.records.length;
  }
}
