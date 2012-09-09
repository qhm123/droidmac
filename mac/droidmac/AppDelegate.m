//
//  AppDelegate.m
//  droidmac
//
//  Created by qhm123 on 12-8-24.
//  Copyright (c) 2012年 qhm123. All rights reserved.
//

#import "AppDelegate.h"
#import "ASIHTTPRequest.h"
#import "JSONKit.h"
#import "Phone.h"
#import "NaviDataSource.h"
#import "NaviDelegate.h"

@implementation AppDelegate
@synthesize rightContentView;
@synthesize appView;
@synthesize outline;
@synthesize tabControl;
@synthesize go;
@synthesize window;
@synthesize urlText;
@synthesize webView;
@synthesize refresh;
@synthesize history;
@synthesize deviceModel;
@synthesize deviceVersion;
@synthesize testView;
@synthesize phoneView;

int bytesReceived;
NSURLResponse *downloadResponse;

- (void)applicationDidFinishLaunching:(NSNotification *)aNotification
{
    [rightContentView addSubview:phoneView];
    
    [webView setDownloadDelegate:self];
    [webView setPolicyDelegate:self];
    [webView setUIDelegate:self];
    
    NSInteger count = [self getConnectDeviceCount];
    if(count != 1) {
        NSAlert *alert = [[NSAlert alloc] init];
        [alert setMessageText:@"没有连接设备"];
        [alert setInformativeText:@""];
        [alert runModal];
    } else {
        BOOL result = [self startDroid];
        if(!result) {
            NSBundle *mainBundle = [NSBundle mainBundle];
            NSString *path = [mainBundle pathForResource:@"droidmac.apk" ofType:nil];
            
            NSArray *arguments = [NSArray arrayWithObjects: @"install", path, nil];
            [self execAdb:arguments];
        } else {
            NSAlert *alert = [[NSAlert alloc] init];
            [alert setMessageText:@"手机上已安装守护程序，或者安装守护程序失败"];
            [alert setInformativeText:@""];
            [alert runModal];
        }
        
        [self adbForward];
        
        [self updateDeviceInfo];
    }
}

- (NSInteger) getConnectDeviceCount {
    NSArray *arguments = [NSArray arrayWithObjects: @"devices", nil];
    NSString *result = [self execAdb:arguments];

    NSArray *array = [result componentsSeparatedByString:@"\n"];
    NSLog(@"device %@, size: %ld", result, [array count]);
    
    return [array count] - 3;
}

- (NSString*) execAdb:(NSArray *)arguments
{
    NSBundle *mainBundle = [NSBundle mainBundle];
    NSString *aaptPath = [mainBundle pathForResource:@"adb" ofType:nil];
    
    NSTask *task = [[NSTask alloc] init];
    [task setLaunchPath: aaptPath];
    [task setArguments: arguments];
    
    NSPipe *pipe = [NSPipe pipe];
    [task setStandardOutput: pipe];
    NSFileHandle *file = [pipe fileHandleForReading];
    
    [task launch];
    
    NSData *data = [file readDataToEndOfFile];
    
    NSString *string = [[NSString alloc] initWithData: data
                                   encoding: NSUTF8StringEncoding];
    
    NSLog(@"string: %@", string);
    
    return string;
}

- (void) adbForward {
    NSArray *arguments = [NSArray arrayWithObjects: @"forward", @"tcp:8080", @"tcp:8080", nil];
    [self execAdb:arguments];
}

- (BOOL) startDroid {
    NSArray *arguments = [NSArray arrayWithObjects: @"shell", @"am", @"start", @"-a", @"com.droidmac.start", nil];
    NSString *result = [self execAdb:arguments];
    if([result rangeOfString:@"Error"].location != NSNotFound) {
        return NO;
    }
    
    return YES;
}

- (IBAction)goClick:(id)sender {
    NSString *urlAddress = [urlText stringValue];
    NSURL *url = [NSURL URLWithString:urlAddress];
    NSURLRequest *requestObj = [NSURLRequest requestWithURL:url];
    [[webView mainFrame] loadRequest:requestObj];
}

- (IBAction)historyClick:(id)sender {

}

- (IBAction)refreshClick:(id)sender {
    [[webView mainFrame] reload];
}

- (void)updateDeviceInfo {
    NSURL *url = [NSURL URLWithString:@"http://localhost:8080/phone"];
    ASIHTTPRequest *request = [ASIHTTPRequest requestWithURL:url];
    [request startSynchronous];
    NSError *error = [request error];
    if (!error) {
        NSString *response = [request responseString];
        NSLog(@"response: %@", response);
        NSDictionary *deserializedData = [response objectFromJSONString];
        NSString *model = [deserializedData objectForKey:@"MODEL"];
        NSLog(@"model: %@", model);
        
        Phone *phone = [Phone fromJson:response];
        NSLog(@"%@, %@", [phone device], [phone model]);
        [deviceModel setStringValue:[phone model]];
        NSString *version = [[NSString alloc] initWithFormat:@"Android SDK %@", [phone sdk]];
        [deviceVersion setStringValue: version];
    }
}

- (void)webView:(WebView *)webView
decidePolicyForMIMEType:(NSString *)type
        request:(NSURLRequest *)request
          frame:(WebFrame *)frame
decisionListener:(id < WebPolicyDecisionListener >)listener
{
    NSLog(@"type: %@, url: %@", type, [[request URL] path]);
    if([type isEqualToString:@"application/vnd.android.package-archive"])
    {
        /*
        NSURLDownload  *theDownload = [[NSURLDownload alloc] initWithRequest:request delegate:(id)self];
        if (theDownload) {
            // Set the destination file.
            //[theDownload setDestination:@"/tmp/tmp.apk" allowOverwrite:YES];
        } else {
            // inform the user that the download failed.
        }
         */
        NSAlert *alert = [[NSAlert alloc] init];
        [alert setMessageText:@"点击开始下载"];
        [alert runModal];
        
        [listener download];
    }
    //just ignore all other types; the default behaviour will be used
}


- (NSWindow *) downloadWindowForAuthenticationSheet:(WebDownload *)download {
    NSLog(@"downloadWindowForAuthenticationSheet");
    return window;
}

- (void)download:(NSURLDownload *)download didFailWithError:(NSError *)error
{
    // Inform the user.
    NSLog(@"Download failed! Error - %@ %@",
          [error localizedDescription],
          [[error userInfo] objectForKey:NSURLErrorFailingURLStringErrorKey]);
}

- (void)download:(NSURLDownload *)download decideDestinationWithSuggestedFilename:(NSString *)filename
{
    NSString *destinationFilename = [@"/tmp" stringByAppendingPathComponent:filename];
    [download setDestination:destinationFilename allowOverwrite:NO];
}

-(void)download:(NSURLDownload *)download didCreateDestination:(NSString *)path
{
    // path now contains the destination path
    // of the download, taking into account any
    // unique naming caused by -setDestination:allowOverwrite:
    NSLog(@"Final file destination: %@",path);
}

- (void)setDownloadResponse:(NSURLResponse *)aDownloadResponse
{
    downloadResponse = aDownloadResponse;
}

- (void)download:(NSURLDownload *)download didReceiveResponse:(NSURLResponse *)response
{
    // Reset the progress, this might be called multiple times.
    // bytesReceived is an instance variable defined elsewhere.
    bytesReceived = 0;
    
    // Retain the response to use later.
    [self setDownloadResponse:response];
}

- (void)download:(NSURLDownload *)download didReceiveDataOfLength:(unsigned)length
{
    long long expectedLength = [downloadResponse expectedContentLength];
    
    bytesReceived = bytesReceived + length;
    
    if (expectedLength != NSURLResponseUnknownLength) {
        // If the expected content length is
        // available, display percent complete.
        float percentComplete = (bytesReceived/(float)expectedLength)*100.0;
        NSLog(@"Percent complete - %f",percentComplete);
    } else {
        // If the expected content length is
        // unknown, just log the progress.
        NSLog(@"Bytes received - %d",bytesReceived);
    }
}

- (void)downloadDidFinish:(NSURLDownload *)download
{
    // Do something with the data.
    NSLog(@"%@",@"downloadDidFinish");
    NSAlert *alert = [[NSAlert alloc] init];
    [alert setMessageText:@"下载完毕, 还不能自动安装 :-("];
    [alert runModal];
}

- (WebView *)webView:(WebView *)sender createWebViewWithRequest:(NSURLRequest *)request
{
    [[webView mainFrame] loadRequest:request];
    return webView;
}


- (IBAction)tabSwitch:(id)sender {
    NSInteger selected = [tabControl selectedSegment];
    NSLog(@"selected: %ld", selected);
}

#pragma outline delegate

- (NSView *)outlineView:(NSOutlineView *)outlineView viewForTableColumn:(NSTableColumn *)tableColumn item:(id)item {
    NSLog(@"outlineView: %@", item);
    
    /*
    NSTextField *textField = [[NSTextField alloc] init];
    [textField setStringValue:item];
    return textField;
     */

    NSTextField *result = [outlineView makeViewWithIdentifier:@"MainCell" owner:self];
    [result setStringValue:item];
    return result;
}

-(void)outlineViewSelectionDidChange:(NSNotification *)notification
{
    NSLog(@"selection changed");
    if ([outline selectedRow] != -1) {
        NSString *item = [outline itemAtRow:[outline selectedRow]];
            [self _setContentViewToName:item];
    }
}


- (void)_setContentViewToName:(NSString *)name {
    NSView *curView = [[rightContentView subviews] objectAtIndex:0];
    NSArray *items = [[NSArray alloc] initWithObjects:@"设备", @"应用", @"联系人", @"短信", @"媒体", nil];
    NSView *newView;
    NSLog(@"index: %ld", [items indexOfObject:name]);
    switch ([items indexOfObject:name]) {
        case 0:
            newView = phoneView;
            break;
        case 1: {
            NSAlert *alert = [[NSAlert alloc] init];
            [alert setMessageText:@"下载文件时，软件会自动检测是否是apk文件，如果是则立即安装到Android设备上"];
            [alert runModal];
            newView = appView;
        }
            break;
        default:
            newView = testView;
            break;
    }
    [rightContentView replaceSubview:curView with:newView];
}


@end
