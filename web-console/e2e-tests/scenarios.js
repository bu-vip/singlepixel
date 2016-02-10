'use strict';

/* https://github.com/angular/protractor/blob/master/docs/toc.md */

describe('my app', function() {


  it('should automatically redirect to /configView when location hash/fragment is empty', function() {
    browser.get('index.html');
    expect(browser.getLocationAbsUrl()).toMatch("/configView");
  });


  describe('configView', function() {

    beforeEach(function() {
      browser.get('index.html#/configView');
    });


    it('should render configView when user navigates to /configView', function() {
      expect(element.all(by.css('[ng-view] p')).first().getText()).
        toMatch(/partial for view 1/);
    });

  });


  describe('rawDataView', function() {

    beforeEach(function() {
      browser.get('index.html#/rawDataView');
    });


    it('should render rawDataView when user navigates to /rawDataView', function() {
      expect(element.all(by.css('[ng-view] p')).first().getText()).
        toMatch(/partial for view 2/);
    });

  });
});
