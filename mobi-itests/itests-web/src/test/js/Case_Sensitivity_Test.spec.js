/*-
 * #%L
 * itests-web
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2016 - 2019 iNovex Information Systems, Inc.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
var adminUsername = "admin"
var adminPassword = "admin"

module.exports = {
  '@tags': ['login', 'sanity'],

  'Step 1: login as admin' : function(browser) {
    browser
        .url('https://localhost:' +browser.globals.globalPort+ '/mobi/index.html#/home')
        .useXpath()
      .waitForElementVisible('//div[@class="form-group"]//input[@id="username"]')
      .waitForElementVisible('//div[@class="form-group"]//input[@id="password"]')
      .setValue('//div[@class="form-group"]//input[@id="username"]', adminUsername)
      .setValue('//div[@class="form-group"]//input[@id="password"]', adminPassword)
      .click('//button[@type="submit"]')
  },

  'Step 2: check for visibility of home element' : function(browser) {
    browser
        .useCss()
        .waitForElementVisible('.home-page')
  },

  'Step 3: Navigate to administration page' : function(browser) {
    browser
      .useXpath()
      .assert.visible('//*[@ui-sref="root.user-management"]/span[text()[contains(.,"Administration")]]')
      .click('//*[@ui-sref="root.user-management"]/span[text()[contains(.,"Administration")]]')
  },

  'Step 4: check for and compare nav username text' : function(browser) {
    browser
      .useCss()
      .assert.visible('a.current-user-box')
      .getText('a.current-user-box', function(result) {browser.assert.ok(result.value == adminUsername)})
  },

  'Step 5: logout' : function(browser){
    browser
      .useXpath()
      .click('//i[@class= "fa fa-sign-out fa-fw"]/following-sibling::span[text()[contains(.,"Logout")]]')
  },

  'Step 6: login as all caps admin' : function(browser) {
    browser
      .waitForElementVisible('//div[@class="form-group"]//input[@id="username"]')
      .waitForElementVisible('//div[@class="form-group"]//input[@id="password"]')
      .setValue('//div[@class="form-group"]//input[@id="username"]', adminUsername.toUpperCase() )
      .setValue('//div[@class="form-group"]//input[@id="password"]', adminPassword)
      .click('//button[@type="submit"]')
  },

  'Step 7: check for administration nav item' : function(browser) {
    browser
      .assert.visible('//*[@ui-sref="root.user-management"]/span[text()[contains(.,"Administration")]]')
  },

  'Step 8: check for and compare nav username text' : function(browser) {
    browser
      .useCss()
      .assert.visible('a.current-user-box')
      .getText('a.current-user-box', function(result) {browser.assert.ok(result.value == adminUsername)})
  },

  'Step 9: The user successfully logs out' : function(browser){
    browser
      .useXpath()
      .click('//i[@class= "fa fa-sign-out fa-fw"]/following-sibling::span[text()[contains(.,"Logout")]]')
      .assert.visible('//div[@class="form-group"]//input[@id="username"]')
      .assert.visible('//div[@class="form-group"]//input[@id="password"]')
  },

};
