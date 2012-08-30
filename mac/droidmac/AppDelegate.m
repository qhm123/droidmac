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

@implementation AppDelegate
@synthesize window;
@synthesize urlText;
@synthesize webView;
@synthesize go;

int bytesReceived;
NSURLResponse *downloadResponse;

- (void)applicationDidFinishLaunching:(NSNotification *)aNotification
{
    [webView setDownloadDelegate:self];
    [webView setPolicyDelegate:self];
    [webView setUIDelegate:self];
    
    BOOL result = [self startDroid];
    if(!result) {
        NSBundle *mainBundle = [NSBundle mainBundle];
        NSString *path = [mainBundle pathForResource:@"droidmac.apk" ofType:nil];
        
        NSArray *arguments = [NSArray arrayWithObjects: @"install", path, nil];
        [self execAdb:arguments];
    }
    
    [self adbForward];
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

- (IBAction)testBtn:(id)sender {
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
        //[Phone fromJSON];
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
}

- (WebView *)webView:(WebView *)sender createWebViewWithRequest:(NSURLRequest *)request
{
    [[webView mainFrame] loadRequest:request];
    return webView;
}


@end
