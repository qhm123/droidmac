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
@property (weak) IBOutlet NSTextField *urlText;
@property (weak) IBOutlet WebView *webView;
@property (weak) IBOutlet NSToolbarItem *go;
- (IBAction)goClick:(id)sender;
- (IBAction)testBtn:(id)sender;

@end
