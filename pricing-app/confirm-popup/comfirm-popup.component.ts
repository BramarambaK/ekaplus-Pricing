import {
  Component,
  OnInit,
  Input,
  SimpleChange,
  ViewChild,
  ElementRef,
  EventEmitter,
  Output
} from '@angular/core';
import { NgbModal, ModalDismissReasons } from '@ng-bootstrap/ng-bootstrap';

@Component({
  selector: 'app-confirm-pricing-popup',
  templateUrl: './confirm-popup.component.html',
  styleUrls: ['./confirm-popup.component.scss']
})
export class ConfirmPopupComponent implements OnInit {
  ngOnInit() {}

  closeResult: string;
  @ViewChild('content') confirm: ElementRef;

  @Input() title = 'Message';
  @Input() body;
  @Input() btn1;
  @Input() btn2;
  @Output() action = new EventEmitter<boolean>();

  constructor(private modalService: NgbModal) {}

  open(body, content = this.confirm) {
    this.body = body;
    this.modalService
      .open(content, { ariaLabelledBy: 'modal-basic-title' })
      .result.then(
        result => {
          this.closeResult = `Closed with: ${result}`;
        },
        reason => {
          this.closeResult = `Dismissed ${this.getDismissReason(reason)}`;
        }
      );
  }

  takeAction(act, modal) {
    this.action.emit(act);
    modal.dismiss(act);
  }

  private getDismissReason(reason: any): string {
    if (reason === ModalDismissReasons.ESC) {
      return 'by pressing ESC';
    } else if (reason === ModalDismissReasons.BACKDROP_CLICK) {
      return 'by clicking on a backdrop';
    } else {
      return `with: ${reason}`;
    }
  }
}
