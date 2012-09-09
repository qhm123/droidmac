//
//  AppDelegate.h
//  droidmac
//
//  Created by qhm123 on 12-8-24.
//  Copyright (c) 2012年 qhm123. All rights reserved.
//

#import <Cocoa/Cocoa.h>
#import <WebKit/WebKit.h>

@interface AppDelegate : NSObject <NSApplicationDelegate>

@property (assign) IBOutlet NSWindow *window;

@property (weak) IBOutlet NSOutlineView *outline;
@property (weak) IBOutlet NSSegmentedControl *tabControl;

@property (weak) IBOutlet NSButton *go;
@property (weak) IBOutlet NSTextField *urlText;
@property (weak) IBOutlet WebView *webView;
@property (weak) IBOutlet NSButton *refresh;
@property (weak) IBOutlet NSSegmentedControl *history;

@property (weak) IBOutlet NSTextField *deviceModel;
@property (weak) IBOutlet NSTextField *deviceVersion;

@property (weak) IBOutlet NSView *phoneView;
@property (weak) IBOutlet NSView *appView;

@property (weak) IBOutlet NSView *testView;

@property (weak) IBOutlet NSView *rightContentView;

- (IBAction)goClick:(id)sender;
- (IBAction)historyClick:(id)sender;
- (IBAction)refreshClick:(id)sender;
- (IBAction)tabSwitch:(id)sender;

@end
